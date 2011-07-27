package de.fuberlin.wiwiss.silk.workbench.learning.tree

import de.fuberlin.wiwiss.silk.linkspec.similarity.{Aggregator, Aggregation}

case class AggregationNode(aggregation: String, operators: List[OperatorNode]) extends OperatorNode {
  override val children = operators

  override def updateChildren(children: List[Node]) = {
    AggregationNode(aggregation, children.map(_.asInstanceOf[OperatorNode]))
  }

  def build: Aggregation = {
    Aggregation(
      required = false,
      weight = 1,
      operators = operators.map(_.build),
      aggregator = Aggregator(aggregation, Map.empty)
    )
  }
}

object AggregationNode {
  def load(aggregation: Aggregation) = {
    val aggregatorId = aggregation.aggregator match {
      case Aggregator(id, _) => id
    }

    val operatorNodes = aggregation.operators.map(OperatorNode.load).toList

    AggregationNode(aggregatorId, operatorNodes)
  }
}