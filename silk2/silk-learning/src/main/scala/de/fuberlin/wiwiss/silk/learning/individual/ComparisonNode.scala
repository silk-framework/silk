package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.similarity.{DistanceMeasure, Comparison}

case class ComparisonNode(inputs: SourceTargetPair[InputNode], threshold: Double, weight: Int, metric: FunctionNode[DistanceMeasure]) extends OperatorNode {
  require(inputs.source.isSource && !inputs.target.isSource, "inputs.source.isSource && !inputs.target.isSource")

  override val children = inputs.source :: inputs.target :: metric :: Nil

  override def updateChildren(newChildren: List[Node]) = {
    val inputNodes = newChildren.collect {
      case c: InputNode => c
    }
    val metricNode = newChildren.collect {
      case c: FunctionNode[DistanceMeasure] => c
    }.head

    ComparisonNode(SourceTargetPair.fromSeq(inputNodes), threshold, weight, metricNode)
  }

  override def build = {
    Comparison(
      required = false,
      threshold = threshold,
      weight = weight,
      inputs = inputs.map(_.build),
      metric = metric.build()
    )
  }
}

object ComparisonNode {
  def load(comparison: Comparison) = {
    val sourceInputNode = InputNode.load(comparison.inputs.source, true)
    val targetInputNode = InputNode.load(comparison.inputs.target, false)

    val metricNode = FunctionNode.load(comparison.metric, DistanceMeasure)

    ComparisonNode(SourceTargetPair(sourceInputNode, targetInputNode), comparison.threshold, comparison.weight, metricNode)
  }
}