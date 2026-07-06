package org.silkframework.plugins.templating.jinja

import com.hubspot.jinjava.tree.{ExpressionNode, Node, TagNode}
import com.hubspot.jinjava.tree.parse.ExpressionToken
import org.silkframework.runtime.templating.TemplateMethodUsage

import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.matching.Regex

/**
  * Collects all method usages on a given variable in a Jinja template.
  * Only methods with a single string constant parameter are returned.
  */
class JinjaMethodCollector {

  /**
    * Collects all usages of methods called on the given variable name in the template node.
    */
  def collect(node: Node, variableName: String): Seq[TemplateMethodUsage] = {
    node match {
      case tagNode: TagNode =>
        val fromHelpers = extractMethodUsages(tagNode.getHelpers, variableName)
        val fromChildren = tagNode.getChildren.asScala.flatMap(collect(_, variableName)).toSeq
        fromHelpers ++ fromChildren
      case exprNode: ExpressionNode =>
        val expr = exprNode.getMaster.asInstanceOf[ExpressionToken].getExpr
        extractMethodUsages(expr, variableName)
      case _ =>
        node.getChildren.asScala.flatMap(collect(_, variableName)).toSeq
    }
  }

  private def extractMethodUsages(expression: String, varName: String): Seq[TemplateMethodUsage] = {
    JinjaMethodCollector.methodCallPattern(varName).findAllMatchIn(expression).map { m =>
      TemplateMethodUsage(m.group(1), m.group(2))
    }.toSeq
  }
}

object JinjaMethodCollector {

  // Matches: varName.methodName("param") or varName.methodName('param')
  private def methodCallPattern(varName: String): Regex =
    s"""${Regex.quote(varName)}\\.([a-zA-Z_][a-zA-Z0-9_]*)\\(["']([^"']*)["']\\)""".r
}