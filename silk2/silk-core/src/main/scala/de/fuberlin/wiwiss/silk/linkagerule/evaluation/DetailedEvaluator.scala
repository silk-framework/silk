package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation, SimilarityOperator}
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.output.Link.InputValue
import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

object DetailedEvaluator {
  def apply(condition: LinkageRule, entities: DPair[Entity], limit: Double = -1.0): Option[Link] = {
    condition.operator match {
      case Some(op) => {
        val confidence = evaluateOperator(op, entities, limit)

        if (confidence.value.getOrElse(-1.0) >= limit) {
          Some(new Link(entities.source.uri, entities.target.uri, Some(confidence), Some(entities)))
        }
        else {
          None
        }
      }
      case None => {
        if (limit == -1.0) {
          Some(new Link(entities.source.uri, entities.target.uri, Some(Link.SimpleConfidence(Some(-1.0))), Some(entities)))
        }
        else {
          None
        }
      }
    }
  }

  private def evaluateOperator(operator: SimilarityOperator, entities: DPair[Entity], threshold: Double) = operator match {
    case aggregation: Aggregation => evaluateAggregation(aggregation, entities, threshold)
    case comparison: Comparison => evaluateComparison(comparison, entities, threshold)
  }

  private def evaluateAggregation(agg: Aggregation, entities: DPair[Entity], threshold: Double): Link.AggregatorConfidence = {
    val totalWeights = agg.operators.map(_.weight).sum

    var isNone = false

    val operatorValues = {
      for (operator <- agg.operators) yield {
        val updatedThreshold = agg.aggregator.computeThreshold(threshold, operator.weight.toDouble / totalWeights)
        val value = evaluateOperator(operator, entities, updatedThreshold)
        if (operator.required && value.value.isEmpty) isNone = true

        value
      }
    }

    val weightedValues = for((weight, Some(value)) <- agg.operators.map(_.weight) zip operatorValues.map(_.value)) yield (weight, value)

    val aggregatedValue = agg.aggregator.evaluate(weightedValues)

    if (isNone)
      Link.AggregatorConfidence(None, agg, operatorValues)
    else
      Link.AggregatorConfidence(aggregatedValue, agg, operatorValues)
  }

  private def evaluateComparison(comparison: Comparison, entities: DPair[Entity], threshold: Double): Link.ComparisonConfidence = {
    val distance = comparison.apply(entities, threshold)

    val sourceInput = findInput(comparison.inputs.source)
    val targetInput = findInput(comparison.inputs.target)

    val sourceValue = InputValue(sourceInput, comparison.inputs.source(entities))
    val targetValue = InputValue(targetInput, comparison.inputs.target(entities))

    Link.ComparisonConfidence(distance, comparison, sourceValue, targetValue)
  }

  private def findInput(input: Input): PathInput = input match {
    case input: PathInput => input
    case TransformInput(_, _, inputs) => findInput(inputs.head)
  }
}