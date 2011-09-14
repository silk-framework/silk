package de.fuberlin.wiwiss.silk.linkspec.evaluation

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.linkspec.similarity.{Comparison, Aggregation, SimilarityOperator}
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.output.Link.InputValue
import de.fuberlin.wiwiss.silk.linkspec.input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.instance.Instance
import de.fuberlin.wiwiss.silk.linkspec.LinkageRule

object DetailedEvaluator {
  def apply(condition: LinkageRule, instances: SourceTargetPair[Instance], limit: Double = -1.0): Option[Link] = {
    condition.operator match {
      case Some(op) => {
        val confidence = evaluateOperator(op, instances, limit)

        if (confidence.value.getOrElse(-1.0) >= limit) {
          Some(new Link(instances.source.uri, instances.target.uri, Some(confidence), Some(instances)))
        }
        else {
          None
        }
      }
      case None => {
        if (limit == -1.0) {
          Some(new Link(instances.source.uri, instances.target.uri, Some(Link.SimpleConfidence(Some(-1.0))), Some(instances)))
        }
        else {
          None
        }
      }
    }
  }

  private def evaluateOperator(operator: SimilarityOperator, instances: SourceTargetPair[Instance], threshold: Double) = operator match {
    case aggregation: Aggregation => evaluateAggregation(aggregation, instances, threshold)
    case comparison: Comparison => evaluateComparison(comparison, instances, threshold)
  }

  private def evaluateAggregation(aggregation: Aggregation, instances: SourceTargetPair[Instance], threshold: Double): Link.AggregatorConfidence = {
    val totalWeights = aggregation.operators.map(_.weight).sum

    var isNone = false

    val operatorValues = {
      for (operator <- aggregation.operators) yield {
        val updatedThreshold = aggregation.aggregator.computeThreshold(threshold, operator.weight.toDouble / totalWeights)
        val value = evaluateOperator(operator, instances, updatedThreshold)
        if (operator.required && value.value.isEmpty) isNone = true

        value
      }
    }

    val weightedValues = aggregation.operators.map(_.weight) zip operatorValues.map(_.value.getOrElse(-1.0))

    val aggregatedValue = aggregation.aggregator.evaluate(weightedValues)

    if (isNone)
      Link.AggregatorConfidence(None, aggregation, operatorValues)
    else
      Link.AggregatorConfidence(aggregatedValue, aggregation, operatorValues)
  }

  private def evaluateComparison(comparison: Comparison, instances: SourceTargetPair[Instance], threshold: Double): Link.ComparisonConfidence = {
    val distance = comparison.apply(instances, threshold)

    val sourceInput = findInput(comparison.inputs.source)
    val targetInput = findInput(comparison.inputs.target)

    val sourceValue = InputValue(sourceInput, comparison.inputs.source(instances))
    val targetValue = InputValue(targetInput, comparison.inputs.target(instances))

    Link.ComparisonConfidence(distance, comparison, sourceValue, targetValue)
  }

  private def findInput(input: Input): PathInput = input match {
    case input: PathInput => input
    case TransformInput(_, inputs, _) => findInput(inputs.head)
  }
}