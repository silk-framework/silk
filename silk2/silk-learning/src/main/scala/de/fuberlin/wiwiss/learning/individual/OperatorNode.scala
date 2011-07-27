package de.fuberlin.wiwiss.silk.workbench.learning.tree

import de.fuberlin.wiwiss.silk.linkspec.similarity.{Comparison, Aggregation, SimilarityOperator}

trait OperatorNode extends Node {
  def build: SimilarityOperator
}

object OperatorNode {
  def load(operator: SimilarityOperator): OperatorNode = operator match {
    case aggregation: Aggregation => AggregationNode.load(aggregation)
    case comparison: Comparison => ComparisonNode.load(comparison)
  }
}