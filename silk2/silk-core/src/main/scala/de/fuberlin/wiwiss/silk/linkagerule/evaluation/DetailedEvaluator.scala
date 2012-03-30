/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.linkagerule.evaluation

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation, SimilarityOperator}
import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.DetailedLink._

object DetailedEvaluator {
  def apply(condition: LinkageRule, entities: DPair[Entity], limit: Double = -1.0): Option[DetailedLink] = {
    condition.operator match {
      case Some(op) => {
        val confidence = evaluateOperator(op, entities, limit)

        if (confidence.value.getOrElse(-1.0) >= limit)
          Some(new DetailedLink(entities.source.uri, entities.target.uri, Some(entities), Some(confidence)))
        else
          None
      }
      case None => {
        if (limit == -1.0)
          Some(new DetailedLink(entities.source.uri, entities.target.uri, Some(entities), Some(SimpleConfidence(Some(-1.0)))))
        else
          None
      }
    }
  }

  private def evaluateOperator(operator: SimilarityOperator, entities: DPair[Entity], threshold: Double) = operator match {
    case aggregation: Aggregation => evaluateAggregation(aggregation, entities, threshold)
    case comparison: Comparison => evaluateComparison(comparison, entities, threshold)
  }

  private def evaluateAggregation(agg: Aggregation, entities: DPair[Entity], threshold: Double): DetailedLink.AggregatorConfidence = {
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
      AggregatorConfidence(None, agg, operatorValues)
    else
      AggregatorConfidence(aggregatedValue, agg, operatorValues)
  }

  private def evaluateComparison(comparison: Comparison, entities: DPair[Entity], threshold: Double): DetailedLink.ComparisonConfidence = {
    ComparisonConfidence(
      value = comparison.apply(entities, threshold),
      comparison = comparison,
      sourceValue = evaluateInput(comparison.inputs.source, entities),
      targetValue = evaluateInput(comparison.inputs.target, entities)
    )
  }

  private def evaluateInput(input: Input, entities: DPair[Entity]): Value = input match {
    case ti: TransformInput => {
      val children = ti.inputs.map(i => evaluateInput(i, entities))
      TransformedValue(ti, ti.transformer(children.map(_.values)), children)
    }
    case pi: PathInput => InputValue(pi, input(entities))
  }
}