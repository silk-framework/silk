package org.silkframework.plugins.templating.jinja

import com.hubspot.jinjava.el.ExtendedSyntaxBuilder
import com.hubspot.jinjava.el.ext.{AstDict, AstList, AstNamedParameter}
import com.hubspot.jinjava.lib.tag._
import com.hubspot.jinjava.tree.parse.ExpressionToken
import com.hubspot.jinjava.tree.{ExpressionNode, Node, TagNode}
import com.hubspot.jinjava.util.HelperStringTokenizer
import jinjava.de.odysseus.el.tree.impl.Builder
import jinjava.de.odysseus.el.tree.impl.ast.{AstDot, AstIdentifier, AstMethod, AstNode, AstParameters}
import jinjava.de.odysseus.el.tree.{IdentifierNode, TreeBuilderException, Node => ElNode}
import org.silkframework.runtime.templating.{TemplateVariableName, VariableScope}

import java.lang.reflect.Field
import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, MapHasAsScala, SetHasAsScala}

/**
  * Collects all referenced variables in a Jinja template.
  */
class JinjaVariableCollector  {

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
        scope ++ collectFromExpression(tagNode.getHelpers) ++ collectFromChildren(tagNode, scope)
      case _: ForTag =>
        // Parses expressions of the form "loopVars in loopedVars"
        val parts = tagNode.getHelpers.split("\\s+in\\s+")
        if (parts.length == 2) {
          val loopVars = new HelperStringTokenizer(parts(0)).splitComma(true).allTokens
          // The looped expression is evaluated in the enclosing scope, e.g. it may reference an outer loop variable
          val loopedVars = scope ++ collectFromExpression(parts(1))
          val childVars = collectFromChildren(tagNode, loopedVars.withBoundNames(loopVars.asScala.toSeq))
          val filtedChildVars = childVars.unboundVars.filterNot(v => v.scope == VariableScope("loop") || v.name == "loop" )
          // The loop variables are not bound outside of the loop
          Scope(filtedChildVars, scope.boundVars)
        } else {
          collectFromChildren(tagNode, scope)
        }
      case _: SetTag =>
        val expression = tagNode.getHelpers
        val eqPos = expression.indexOf('=')
        if(eqPos != -1) {
          val leftVars = ArraySeq.unsafeWrapArray(expression.substring(0, eqPos).trim.split("\\s*,\\s*"))
          val rightVars = collectFromExpression("[" + expression.substring(eqPos + 1) + "]")
          scope.withBoundNames(leftVars) ++ rightVars
        } else {
          scope
        }
      case _: MacroTag =>
        // Add all parameters as bound variables to the scope
        val functionScope = scope.withBound(collectFromExpression(tagNode.getHelpers).unboundVars)
        // Collect any unbound variables within the macro
        collectFromChildren(tagNode, functionScope)
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
      // Some nodes do not expose all of their children (e.g. range brackets):
      // identifiers that the walk did not reach are collected as plain variables.
      val hiddenVariables =
        for(identifier <- tree.getIdentifierNodes.asScala.toSeq if
            !walker.visitedIdentifiers.contains(identifier) && !ignoreIdentifier(identifier.getName)) yield {
          new TemplateVariableName(identifier.getName)
        }
      Scope(unboundVars = (walker.variables ++ hiddenVariables).distinct.toSeq)
    } catch {
      case _: TreeBuilderException =>
        // Not a valid expression, so it cannot reference any variables
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
        case method: AstMethod =>
          walkTarget(method.getChild(0), isMethodTarget = true)
          walk(method.getChild(1)) // parameters
        case dot: AstDot =>
          walkTarget(dot, isMethodTarget = false)
        case identifier: AstIdentifier =>
          visitedIdentifiers += identifier
          addVariable(List(identifier.getName))
        case dict: AstDict =>
          for((key, value) <- JinjaVariableCollector.dictEntries(dict)) {
            walk(key)
            walk(value)
          }
        case list: AstList =>
          walk(JinjaVariableCollector.listElements(list))
        case namedParameter: AstNamedParameter =>
          // The name is collected as well: in macro definitions it is a parameter that gets bound
          walk(JinjaVariableCollector.namedParameterName(namedParameter))
          walk(JinjaVariableCollector.namedParameterValue(namedParameter))
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
          dottedPath(dot.getChild(0)).map(_ :+ JinjaVariableCollector.propertyName(dot))
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

    def withUnbound(varNames: Seq[TemplateVariableName]): Scope = {
      copy(unboundVars = (unboundVars ++ varNames).distinct)
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
      Scope(
        unboundVars = (unboundVars ++ scope.unboundVars).distinct.filterNot(isBound),
        boundVars = (boundVars ++ scope.boundVars).distinct
      )
    }

  }

  object Scope {
    def empty: Scope = Scope(Seq.empty, Seq.empty)
  }

}

object JinjaVariableCollector {

  // The Jinjava extension nodes do not expose their children via the Node interface: read the underlying fields directly.
  private val dictField = accessibleField(classOf[AstDict], "dict")
  private val listElementsField = accessibleField(classOf[AstList], "elements")
  private val namedParameterNameField = accessibleField(classOf[AstNamedParameter], "name")
  private val namedParameterValueField = accessibleField(classOf[AstNamedParameter], "value")

  private def accessibleField(cls: Class[_], name: String): Field = {
    val field = cls.getDeclaredField(name)
    field.setAccessible(true)
    field
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

  /** The property name of a dot node. It is only exposed through the `. name` string representation. */
  private def propertyName(dot: AstDot): String = {
    dot.toString.stripPrefix(". ")
  }
}
