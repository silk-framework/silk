package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.similarity.{DistanceMeasure, Comparison}

case class ComparisonNode(inputs: SourceTargetPair[InputNode], threshold: Double, metric: StrategyNode[DistanceMeasure]) extends OperatorNode {
  require(inputs.source.isSource && !inputs.target.isSource, "inputs.source.isSource && !inputs.target.isSource")

  override val children = inputs.source :: inputs.target :: metric :: Nil

  override def updateChildren(newChildren: List[Node]) = {
    val inputNodes = newChildren.collect {
      case c: InputNode => c
    }
    val metricNode = newChildren.collect {
      case c: StrategyNode[DistanceMeasure] => c
    }.head

    ComparisonNode(SourceTargetPair.fromSeq(inputNodes), threshold, metricNode)
  }

  override def build = {
    Comparison(
      required = false,
      threshold = threshold,
      weight = 1,
      inputs = inputs.map(_.build),
      metric = metric.build()
    )
  }
}

object ComparisonNode {
  def load(comparison: Comparison) = {
    val sourceInputNode = InputNode.load(comparison.inputs.source, true)
    val targetInputNode = InputNode.load(comparison.inputs.target, false)

    val metricNode = StrategyNode.load(comparison.metric, DistanceMeasure)

    ComparisonNode(SourceTargetPair(sourceInputNode, targetInputNode), comparison.threshold, metricNode)
  }
}