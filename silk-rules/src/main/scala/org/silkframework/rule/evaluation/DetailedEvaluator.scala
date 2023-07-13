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
  def apply(rule: LinkageRule, entities: DPair[Entity], limit: Double): Option[EvaluatedLink] = {
    rule.operator match {
      case Some(op) =>
        if(rule.isReflexive || entities.source.uri != entities.target.uri) {
          val confidence = evaluateOperator(op, entities, limit)
          if (confidence.score.getOrElse(-1.0) >= limit) {
            Some(new EvaluatedLink(entities.source.uri, entities.target.uri, entities, confidence))
          } else {
            None
          }
        } else {
          None
        }

      case None =>
        if (limit == -1.0) {
          Some(new EvaluatedLink(entities.source.uri, entities.target.uri, entities, SimpleConfidence(Some(-1.0))))
        } else {
          None
        }
    }
  }

  /**
    * Evaluates a linkage rule.
    */
  def apply(rule: LinkageRule, entities: DPair[Entity]): EvaluatedLink = {
    rule.operator match {
      case Some(op) =>
        val confidence = evaluateOperator(op, entities, -1.0)
        new EvaluatedLink(entities.source.uri, entities.target.uri, entities, confidence)
      case None =>
        new EvaluatedLink(entities.source.uri, entities.target.uri, entities, SimpleConfidence(Some(-1.0)))
    }
  }

  /**
   * Evaluates a set of transform rules.
   */
  def apply(rules: Seq[TransformRule], entity: Entity): DetailedEntity = {
    val subjectRule = rules.find(_.target.isEmpty)
    val uris = subjectRule match {
      case Some(rule) => rule(entity)
      case None => Seq(entity.uri.toString)
    }

    val values = for(rule <- rules) yield apply(rule, entity)
    DetailedEntity(uris, values, rules)
  }

  /**
   * Evaluates a single transform rule.
   */
  def apply(rule: TransformRule, entity: Entity): Value = {
    val result = evaluateInput(rule.operator, entity)
    // Validate values
    for(target <- rule.target) {
      try {
        target.validate(result.values)
      } catch {
        case NonFatal(ex) =>
          return result.withError(ex)
      }
    }
    // Return validated result
    result
  }

  private def evaluateOperator(operator: SimilarityOperator, entities: DPair[Entity], threshold: Double) = operator match {
    case aggregation: Aggregation => evaluateAggregation(aggregation, entities, threshold)
    case comparison: Comparison => evaluateComparison(comparison, entities, threshold)
  }

  private def evaluateAggregation(agg: Aggregation, entities: DPair[Entity], threshold: Double): AggregatorConfidence = {
    val operatorValues = {
      for (operator <- agg.operators) yield {
        evaluateOperator(operator, entities, threshold)
      }
    }

    val aggregatedValue = agg.aggregator(agg.operators, entities, threshold)
    AggregatorConfidence(aggregatedValue.score, agg, operatorValues)
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