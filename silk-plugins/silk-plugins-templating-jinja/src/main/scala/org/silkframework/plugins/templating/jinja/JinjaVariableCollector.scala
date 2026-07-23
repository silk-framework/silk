package org.silkframework.plugins.templating.jinja

import com.hubspot.jinjava.el.ExtendedSyntaxBuilder
import com.hubspot.jinjava.el.ext.{AstDict, AstList, AstMacroFunction, AstNamedParameter, AstRangeBracket}
import com.hubspot.jinjava.lib.tag._
import com.hubspot.jinjava.tree.parse.ExpressionToken
import com.hubspot.jinjava.tree.{ExpressionNode, Node, TagNode}
import com.hubspot.jinjava.util.HelperStringTokenizer
import jinjava.de.odysseus.el.tree.impl.Builder
import jinjava.de.odysseus.el.tree.impl.ast.{AstDot, AstIdentifier, AstMethod, AstNode, AstParameters}
import jinjava.de.odysseus.el.tree.{IdentifierNode, Node => ElNode}
import jinjava.javax.el.ELException
import org.silkframework.runtime.templating.{TemplateVariableName, VariableScope}

import java.lang.reflect.Field
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, MapHasAsScala, SetHasAsScala}

/**
  * Collects all referenced variables in a Jinja template.
  */
class JinjaVariableCollector {

  import JinjaVariableCollector._

  private val EXPRESSION_START_TOKEN = "#{"
  private val EXPRESSION_END_TOKEN = "}"

  // Uses the same features as the Jinjava runtime parser, so every expression that renders can also be analyzed
  private val builder = new ExtendedSyntaxBuilder(Builder.Feature.METHOD_INVOCATIONS, Builder.Feature.VARARGS)

  /**
    * Collects all variable names from a Jinja template node.
    */
  def collect(node: Node, scope: Scope = Scope.empty): Scope = {
    node match {
      case tagNode: TagNode =>
        collectFromTag(tagNode, scope)
      case exprNode: ExpressionNode =>
        scope ++ collectFromExpression(exprNode.getMaster.asInstanceOf[ExpressionToken].getExpr)
      case _ =>
        collectFromChildren(node, scope)
    }
  }

  /**
    * Collects all variable names from a Jinja template tag.
    * Needs to copy code from the individual tags to replicate behaviour.
    */
  private def collectFromTag(tagNode: TagNode, scope: Scope): Scope = {
    tagNode.getTag match {
      case _: IfTag | _: ElseIfTag | _: DoTag =>
        // If blocks are no scopes in Jinja: variables that are set inside remain bound after the block
        var curScope = scope ++ collectFromExpression(tagNode.getHelpers)
        for(child <- tagNode.getChildren.asScala) {
          curScope = collect(child, curScope)
        }
        curScope
      case _: ForTag =>
        // Parses expressions of the form "loopVars in loopedVars"; like Jinjava, splits at the first 'in'
        val parts = tagNode.getHelpers.split("\\s+in\\s+", 2)
        if (parts.length == 2) {
          val loopVars = new HelperStringTokenizer(parts(0)).splitComma(true).allTokens
          // The looped expression is evaluated in the enclosing scope, e.g. it may reference an outer loop variable
          val loopedVars = scope ++ collectFromExpression(parts(1))
          // 'loop' is bound inside the body, so references to the loop state are filtered at any depth
          val childVars = collectFromChildren(tagNode, loopedVars.withBoundNames(loopVars.asScala.toSeq :+ "loop"))
          // The loop variables are not bound outside of the loop
          Scope(childVars.unboundVars, scope.boundVars)
        } else {
          collectFromChildren(tagNode, scope)
        }
      case _: SetTag =>
        val expression = tagNode.getHelpers
        val eqPos = expression.indexOf('=')
        if(eqPos != -1) {
          val leftVars = expression.substring(0, eqPos).trim.split("\\s*,\\s*").toSeq
          val rightVars = collectFromExpression("[" + expression.substring(eqPos + 1) + "]")
          // The right side is evaluated before the assignment, so it may reference the assigned variable itself
          (scope ++ rightVars).withBoundNames(leftVars)
        } else {
          // Block form '{% set x %}...{% endset %}': the rendered body is assigned to the name before the optional filter
          val filterPos = expression.indexOf('|')
          val varName = (if(filterPos == -1) expression else expression.substring(0, filterPos)).trim
          collectFromChildren(tagNode, scope).withBoundNames(Seq(varName))
        }
      case _: MacroTag =>
        val signature = collectMacroSignature(tagNode.getHelpers)
        // Default values are evaluated in the enclosing scope, the parameters are bound within the body
        val bodyScope = (scope ++ Scope(signature.unboundVars)).withBound(signature.boundVars)
        val innerScope = collectFromChildren(tagNode, bodyScope)
        // The macro parameters are not bound outside of the definition
        Scope(innerScope.unboundVars, scope.boundVars)
      case _: CallTag =>
        // The call arguments are evaluated when the tag is rendered
        scope ++ collectFromExpression(tagNode.getHelpers) ++ collectFromChildren(tagNode, scope)
      case _ =>
        collectFromChildren(tagNode, scope)
    }
  }

  private def collectFromChildren(node: Node, scope: Scope): Scope = {
    var curScope = scope
    for(child <- node.getChildren.asScala) {
      val newScope = collect(child, curScope)
      curScope = newScope
    }
    // Any newly bound variable is not valid outside of this node's children
    curScope.copy(boundVars = scope.boundVars)
  }

  /**
    * Parses an expression from a Jinja template and collects all variable references from its AST.
    * Expressions are used in tags, such as in if and for expressions.
    */
  private def collectFromExpression(expression: String): Scope = {
    try {
      val tree = builder.build(EXPRESSION_START_TOKEN + expression + EXPRESSION_END_TOKEN)
      val walker = new ExpressionWalker()
      walker.walk(tree.getRoot)
      // Some nodes do not expose all of their children:
      // identifiers that the walk did not reach are collected as plain variables.
      val hiddenVariables =
        for(identifier <- tree.getIdentifierNodes.asScala.toSeq if
            !walker.visitedIdentifiers.contains(identifier) && !ignoreIdentifier(identifier.getName)) yield {
          new TemplateVariableName(identifier.getName)
        }
      Scope(unboundVars = (walker.variables ++ hiddenVariables).distinct.toSeq)
    } catch {
      case _: ELException =>
        // Not a valid expression, so it cannot reference any variables
        Scope.empty
    }
  }

  /**
    * Parses a macro signature of the form 'name(param, paramWithDefault=expr)'.
    * Returns the parameter names as bound variables and the free variables of default values as unbound variables.
    */
  private def collectMacroSignature(expression: String): Scope = {
    try {
      val tree = builder.build(EXPRESSION_START_TOKEN + expression + EXPRESSION_END_TOKEN)
      tree.getRoot.getChild(0) match {
        case macroFunction: AstMacroFunction =>
          val walker = new ExpressionWalker()
          val parameterNames = Seq.newBuilder[TemplateVariableName]
          val parameters = macroFunction.getChild(0)
          for(i <- 0 until parameters.getCardinality) {
            parameters.getChild(i) match {
              case identifier: AstIdentifier =>
                parameterNames += new TemplateVariableName(identifier.getName)
              case namedParameter: AstNamedParameter =>
                parameterNames += new TemplateVariableName(namedParameterName(namedParameter).getName)
                walker.walk(namedParameterValue(namedParameter))
              case other =>
                walker.walk(other)
            }
          }
          Scope(unboundVars = walker.variables.distinct.toSeq, boundVars = parameterNames.result())
        case _ =>
          Scope.empty
      }
    } catch {
      case _: ELException =>
        Scope.empty
    }
  }

  private def ignoreIdentifier(name: String): Boolean = {
    name.startsWith("___") || // internal identifier
    name.startsWith("filter:") || // Jinja filter
    name.startsWith("exptest:") // Jinja test
  }

  /**
    * Collects variable references by walking an expression AST.
    * Dotted chains rooted at an identifier (e.g. 'project.myVar') are a single scoped variable reference.
    * For method calls (e.g. 'name.trim()'), the method segment is not part of the reference.
    */
  private class ExpressionWalker {

    /** All collected variable references in document order. */
    val variables = mutable.ArrayBuffer[TemplateVariableName]()

    /** All identifier nodes reached by the walk (by identity), including deliberately ignored ones. */
    val visitedIdentifiers: mutable.Set[IdentifierNode] =
      java.util.Collections.newSetFromMap(new java.util.IdentityHashMap[IdentifierNode, java.lang.Boolean]()).asScala

    def walk(node: ElNode): Unit = {
      node match {
        case null =>
          // Nodes may report null children, e.g. a bracket with an omitted range start
        case method: AstMethod =>
          walkTarget(method.getChild(0), isMethodTarget = true)
          walk(method.getChild(1)) // parameters
        case dot: AstDot =>
          walkTarget(dot, isMethodTarget = false)
        case identifier: AstIdentifier =>
          visitedIdentifiers += identifier
          addVariable(List(identifier.getName))
        case rangeBracket: AstRangeBracket =>
          // The range maximum is not exposed as a child
          walk(rangeBracket.getChild(0))
          walk(rangeBracket.getChild(1))
          walk(rangeMax(rangeBracket))
        case dict: AstDict =>
          for((key, value) <- dictEntries(dict)) {
            key match {
              case identifier: AstIdentifier =>
                // Identifier keys are literal key names, not variable references
                visitedIdentifiers += identifier
              case other =>
                walk(other)
            }
            walk(value)
          }
        case list: AstList =>
          walk(listElements(list))
        case namedParameter: AstNamedParameter =>
          // The parameter name is not a variable reference; macro definitions bind it via collectMacroSignature
          visitedIdentifiers += namedParameterName(namedParameter)
          walk(namedParameterValue(namedParameter))
        case other =>
          for(i <- 0 until other.getCardinality) {
            walk(other.getChild(i))
          }
      }
    }

    /** Walks a dotted property chain. If it is rooted at an identifier, the whole chain is a single variable reference. */
    private def walkTarget(node: ElNode, isMethodTarget: Boolean): Unit = {
      dottedPath(node) match {
        case Some(path) =>
          addVariable(if(isMethodTarget) path.init else path)
        case None =>
          node match {
            case dot: AstDot =>
              walk(dot.getChild(0)) // property access on a computed value is not a variable reference
            case other =>
              walk(other)
          }
      }
    }

    /** The segments of a dotted chain, or None if it is not rooted at a plain identifier. */
    private def dottedPath(node: ElNode): Option[List[String]] = {
      node match {
        case dot: AstDot =>
          dottedPath(dot.getChild(0)).map(_ :+ propertyName(dot))
        case identifier: AstIdentifier =>
          visitedIdentifiers += identifier
          Some(List(identifier.getName))
        case _ =>
          None
      }
    }

    private def addVariable(path: List[String]): Unit = {
      if(path.nonEmpty && !ignoreIdentifier(path.head)) {
        variables += new TemplateVariableName(path.last, VariableScope(path.init))
      }
    }
  }

}

object JinjaVariableCollector {

  /**
    * Holds all bound and unbound variables at a specific node in the AST.
    */
  case class Scope(unboundVars: Seq[TemplateVariableName], boundVars: Seq[TemplateVariableName] = Seq.empty) {

    def withBoundNames(varNames: Seq[String]): Scope = {
      withBound(varNames.map(new TemplateVariableName(_)))
    }

    def withBound(varNames: Seq[TemplateVariableName]): Scope = {
      copy(boundVars = (boundVars ++ varNames).distinct)
    }

    /**
      * Adds a scope from a subsequent node.
      */
    def ++(scope: Scope): Scope = {
      val boundVarsSet = boundVars.toSet
      val boundSimpleNames = boundVars.filter(_.scope.isEmpty).map(_.name).toSet
      def isBound(v: TemplateVariableName): Boolean = {
        boundVarsSet.contains(v) || v.scope.path.headOption.exists(boundSimpleNames.contains)
      }
      // Only the added scope is filtered: bindings never apply to references that were collected before them
      Scope(
        unboundVars = (unboundVars ++ scope.unboundVars.filterNot(isBound)).distinct,
        boundVars = (boundVars ++ scope.boundVars).distinct
      )
    }

  }

  object Scope {
    def empty: Scope = Scope(Seq.empty, Seq.empty)
  }

  // The Jinjava extension nodes do not expose these children via the Node interface: read the underlying fields directly.
  private val dictField = accessibleField(classOf[AstDict], "dict")
  private val listElementsField = accessibleField(classOf[AstList], "elements")
  private val namedParameterNameField = accessibleField(classOf[AstNamedParameter], "name")
  private val namedParameterValueField = accessibleField(classOf[AstNamedParameter], "value")
  private val rangeMaxField = accessibleField(classOf[AstRangeBracket], "rangeMax")
  private val dotPropertyField = accessibleField(classOf[AstDot], "property")

  private def accessibleField(cls: Class[_], name: String): Field = {
    try {
      val field = cls.getDeclaredField(name)
      field.setAccessible(true)
      field
    } catch {
      case ex: ReflectiveOperationException =>
        throw new IllegalStateException(s"Jinjava internals changed: field ${cls.getName}.$name is not accessible.", ex)
    }
  }

  private def dictEntries(dict: AstDict): Seq[(AstNode, AstNode)] = {
    dictField.get(dict).asInstanceOf[java.util.Map[AstNode, AstNode]].asScala.toSeq
  }

  private def listElements(list: AstList): AstParameters = {
    listElementsField.get(list).asInstanceOf[AstParameters]
  }

  private def namedParameterName(namedParameter: AstNamedParameter): AstIdentifier = {
    namedParameterNameField.get(namedParameter).asInstanceOf[AstIdentifier]
  }

  private def namedParameterValue(namedParameter: AstNamedParameter): AstNode = {
    namedParameterValueField.get(namedParameter).asInstanceOf[AstNode]
  }

  private def rangeMax(rangeBracket: AstRangeBracket): AstNode = {
    rangeMaxField.get(rangeBracket).asInstanceOf[AstNode]
  }

  private def propertyName(dot: AstDot): String = {
    dotPropertyField.get(dot).asInstanceOf[String]
  }
}
