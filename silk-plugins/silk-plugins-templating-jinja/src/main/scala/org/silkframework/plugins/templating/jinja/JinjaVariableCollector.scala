package org.silkframework.plugins.templating.jinja

import com.hubspot.jinjava.el.ExtendedSyntaxBuilder
import com.hubspot.jinjava.lib.tag._
import com.hubspot.jinjava.tree.parse.ExpressionToken
import com.hubspot.jinjava.tree.{ExpressionNode, Node, TagNode}
import com.hubspot.jinjava.util.HelperStringTokenizer
import jinjava.de.odysseus.el.tree.TreeBuilderException
import jinjava.de.odysseus.el.tree.impl.ast.{AstDot, AstEval}
import org.silkframework.runtime.templating.TemplateVariableName

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala}

/**
  * Collects all referenced variables in a Jinja template.
  */
class JinjaVariableCollector  {

  private val EXPRESSION_START_TOKEN = "#{"
  private val EXPRESSION_END_TOKEN = "}"

  private val builder = new ExtendedSyntaxBuilder

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
          val loopedVars = collectFromExpression(parts(1))
          val childVars = collectFromChildren(tagNode, scope.withBoundNames(loopVars.asScala.toSeq))
          val filtedChildVars = childVars.unboundVars.filterNot(v => v.scope == Seq("loop") || v.name == "loop" )
          loopedVars.withUnbound(filtedChildVars)
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
    * Parses an expression from a Jinja template and collects all variable names.
    * Expressions are used in tags, such as in if and for expressions.
    */
  private def collectFromExpression(expression: String): Scope = {
    try {
      val tree = builder.build(EXPRESSION_START_TOKEN + expression + EXPRESSION_END_TOKEN)
      // Manually treat simple expressions of the form `project.variable` or `variable.method(...)`
      expression match {
        case JinjaVariableCollector.scopedName(scopePart, name) =>
          val scope = scopePart.dropRight(1).split('.').toSeq
          Scope(
            unboundVars = Seq(new TemplateVariableName(name, scope))
          )
        case JinjaVariableCollector.methodCallOnVar(varName) =>
          Scope(
            unboundVars = Seq(new TemplateVariableName(varName, Seq.empty))
          )
        case _ =>
          Scope(
            unboundVars = tree.getIdentifierNodes.asScala.map(_.getName).filterNot(ignoreIdentifierNode).toSeq.map(new TemplateVariableName(_, Seq.empty))
          )
      }
    } catch {
      case _: TreeBuilderException =>
        // Fallback: try to extract the leading variable from method call expressions like `var.method(...)`
        expression match {
          case JinjaVariableCollector.methodCallOnVar(varName) =>
            Scope(unboundVars = Seq(new TemplateVariableName(varName, Seq.empty)))
          case _ =>
            Scope.empty
        }
    }
  }

  private def ignoreIdentifierNode(name: String): Boolean = {
    name.startsWith("___") || // internal identifier
    name.startsWith("filter:") || // Jinja filter
    name.startsWith("exptest:") // Jinja test
  }

  /**
    * Holds all bound and unbound variables at a specific node in the AST.
    */
  case class Scope(unboundVars: Seq[TemplateVariableName], boundVars: Seq[TemplateVariableName] = Seq.empty) {

    def withBoundNames(varNames: Seq[String]): Scope = {
      withBound(varNames.map(new TemplateVariableName(_, Seq.empty)))
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
      Scope(
        unboundVars = (unboundVars ++ scope.unboundVars).distinct.filterNot(boundVarsSet),
        boundVars = (boundVars ++ scope.boundVars).distinct
      )
    }

  }

  object Scope {
    def empty: Scope = Scope(Seq.empty, Seq.empty)
  }

}

object JinjaVariableCollector {

  // Regex for valid variable names
  private val variableRegex = "[a-zA-Z_][a-zA-Z0-9_]*".r

  // Regex for scoped names of the form scope1[.scope2]*.var
  private val scopedName = s"((?:$variableRegex\\.)+)($variableRegex)".r

  // Regex for method calls on a variable of the form var.method(...)
  private val methodCallOnVar = s"($variableRegex)\\.$variableRegex\\(.*\\)".r

}
