package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

case class LinkageRuleNode(aggregation: Option[OperatorNode]) extends Node {
  override val children = aggregation.toList

  override def updateChildren(children: List[Node]) = {
    LinkageRuleNode(children.headOption.map(_.asInstanceOf[OperatorNode]))
  }

  def build = LinkageRule(aggregation.map(_.build))

  override def toString = build.toString
}

object LinkageRuleNode {
  def load(linkageRule: LinkageRule) = {
    LinkageRuleNode(linkageRule.operator.map(OperatorNode.load))
  }
}
