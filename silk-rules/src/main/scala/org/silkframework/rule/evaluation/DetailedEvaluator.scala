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

package org.silkframework.rule.evaluation

import org.silkframework.entity.Entity
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.rule.{LinkageRule, TransformRule}
import org.silkframework.util.DPair

import scala.util.control.NonFatal

object DetailedEvaluator {

  /**
   * Evaluates a linkage rule.
   */
  def apply(rule: LinkageRule, entities: DPair[Entity], limit: Double = -1.0): Option[DetailedLink] = {
    rule.operator match {
      case Some(op) =>
        val confidence = evaluateOperator(op, entities, limit)
        if (confidence.score.getOrElse(-1.0) >= limit)
          Some(new DetailedLink(entities.source.uri, entities.target.uri, Some(entities), Some(confidence)))
        else
          None

      case None =>
        if (limit == -1.0)
          Some(new DetailedLink(entities.source.uri, entities.target.uri, Some(entities), Some(SimpleConfidence(Some(-1.0)))))
        else
          None
    }
  }

  /**
   * Evaluates a set of transform rules.
   */
  def apply(rules: Seq[TransformRule], entity: Entity): DetailedEntity = {
    val subjectRule = rules.find(_.target.isEmpty)
    val propertyRules = rules.filter(_.target.isDefined)

    val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
    val values = for(rule <- propertyRules) yield evaluateInput(rule.operator, entity)
    DetailedEntity(uri, values, propertyRules)
  }

  /**
   * Evaluates a single transform rule.
   */
  def apply(rule: TransformRule, entity: Entity): Option[Value] = {
    Some(evaluateInput(rule.operator, entity))
  }

  private def evaluateOperator(operator: SimilarityOperator, entities: DPair[Entity], threshold: Double) = operator match {
    case aggregation: Aggregation => evaluateAggregation(aggregation, entities, threshold)
    case comparison: Comparison => evaluateComparison(comparison, entities, threshold)
  }

  private def evaluateAggregation(agg: Aggregation, entities: DPair[Entity], threshold: Double): AggregatorConfidence = {
    val totalWeights = agg.operators.map(_.weight).sum

    var isNone = false

    val operatorValues = {
      for (operator <- agg.operators) yield {
        val updatedThreshold = agg.aggregator.computeThreshold(threshold, operator.weight.toDouble / totalWeights)
        val value = evaluateOperator(operator, entities, updatedThreshold)
        if (operator.required && value.score.isEmpty) isNone = true

        value
      }
    }

    val weightedValues = for((weight, Some(value)) <- agg.operators.map(_.weight) zip operatorValues.map(_.score)) yield (weight, value)

    val aggregatedValue = agg.aggregator.evaluate(weightedValues)

    if (isNone)
      AggregatorConfidence(None, agg, operatorValues)
    else
      AggregatorConfidence(aggregatedValue, agg, operatorValues)
  }

  private def evaluateComparison(comparison: Comparison, entities: DPair[Entity], threshold: Double): ComparisonConfidence = {
    ComparisonConfidence(
      score = comparison.apply(entities, threshold),
      comparison = comparison,
      sourceValue = evaluateInput(comparison.inputs.source, entities.source),
      targetValue = evaluateInput(comparison.inputs.target, entities.target)
    )
  }

  private def evaluateInput(input: Input, entity: Entity): Value = input match {
    case ti: TransformInput =>
      val children = ti.inputs.map(i => evaluateInput(i, entity))
      try {
        TransformedValue(ti, ti.transformer(children.map(_.values)), children)
      } catch {
        case NonFatal(ex) => TransformedValue(ti, Seq.empty, children, Some(ex))
      }

    case pi: PathInput => InputValue(pi, input(entity))
  }
}