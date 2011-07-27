package de.fuberlin.wiwiss.silk.workbench.learning.tree

import de.fuberlin.wiwiss.silk.linkspec.LinkCondition

case class LinkConditionNode(aggregation: Option[OperatorNode]) extends Node {
  override val children = aggregation.toList

  override def updateChildren(children: List[Node]) = {
    LinkConditionNode(children.headOption.map(_.asInstanceOf[OperatorNode]))
  }

  def build = LinkCondition(aggregation.map(_.build))

  override def toString = build.toString
}

object LinkConditionNode {
  def load(linkCondition: LinkCondition) = {
    LinkConditionNode(linkCondition.rootOperator.map(OperatorNode.load))
  }
}
