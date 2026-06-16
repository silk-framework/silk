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
import org.silkframework.rule.input.{InputExecution, PathInput, TransformInputExecution}
import org.silkframework.rule.similarity.{AggregationExecution, ComparisonExecution, SimilarityOperatorExecution}
import org.silkframework.rule.{ComplexUriMapping, LinkageRuleExecution, TransformRuleExecution}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{DPair, Uri}

import scala.collection.immutable.ArraySeq
import scala.util.control.NonFatal

/**
 * Walks a pre-built rule execution tree to produce a detailed evaluation result
 */
object DetailedEvaluator {

  /**
   * Evaluates a pre-built linkage rule execution against an entity pair.
   */
  def apply(ruleExec: LinkageRuleExecution, entities: DPair[Entity], limit: Double): Option[EvaluatedLink] = {
    val rule = ruleExec.operator
    ruleExec.operatorExecution match {
      case Some(opExec) =>
        if(!rule.excludeSelfReferences || entities.source.uri != entities.target.uri) {
          val confidence = evaluateOperator(opExec, entities, limit)
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
   * Evaluates a pre-built linkage rule execution against an entity pair.
   */
  def apply(ruleExec: LinkageRuleExecution, entities: DPair[Entity]): EvaluatedLink = {
    ruleExec.operatorExecution match {
      case Some(opExec) =>
        val confidence = evaluateOperator(opExec, entities, -1.0)
        new EvaluatedLink(entities.source.uri, entities.target.uri, entities, confidence)
      case None =>
        new EvaluatedLink(entities.source.uri, entities.target.uri, entities, SimpleConfidence(Some(-1.0)))
    }
  }

  /**
   * Evaluates a set of pre-built transform rule executions for a single entity.
   */
  def apply(ruleExecs: Seq[TransformRuleExecution], entity: Entity): DetailedEntity = {
    val subjectExec = ruleExecs.find(_.operator.target.isEmpty)
    val uris = subjectExec match {
      case Some(exec) => exec(entity).values
      case None => Seq(entity.uri.toString)
    }
    val values = ruleExecs.map(re => apply(re, entity))
    DetailedEntity(uris, values, ruleExecs.map(_.operator))
  }

  /**
   * Evaluates a single pre-built transform rule execution for an entity.
   */
  def apply(ruleExec: TransformRuleExecution, entity: Entity): Value = {
    val result = evaluateInput(ruleExec.inputExecution, entity)
    // Validate against the rule's mapping target
    for(target <- ruleExec.operator.target) {
      try {
        target.validate(result.values)
      } catch {
        case NonFatal(ex) =>
          return result.withError(ex)
      }
    }
    // Complex URI mapping rules need to be validated separately
    if(ruleExec.operator.isInstanceOf[ComplexUriMapping]) {
      val invalidUri = result.values.find(uri => !Uri(uri).isValidUri)
      if(invalidUri.isDefined) {
        // The URI rule has generated an invalid URI
        return result.withError(new ValidationException(s"URI rule of object mapping has generated an invalid URI: '${invalidUri.get}'!"))
      }
    }
    result
  }

  private def evaluateOperator(opExec: SimilarityOperatorExecution, entities: DPair[Entity], threshold: Double): Confidence = opExec match {
    case agg: AggregationExecution => evaluateAggregation(agg, entities, threshold)
    case cmp: ComparisonExecution => evaluateComparison(cmp, entities, threshold)
  }

  private def evaluateAggregation(agg: AggregationExecution, entities: DPair[Entity], threshold: Double): AggregatorConfidence = {
    val operatorValues = agg.operatorExecutions.map(opExec => evaluateOperator(opExec, entities, threshold))
    val aggregatedValue = agg.operator.aggregator(agg.operatorExecutions, entities, threshold)
    AggregatorConfidence(aggregatedValue.score, agg.operator, operatorValues)
  }

  private def evaluateComparison(cmp: ComparisonExecution, entities: DPair[Entity], threshold: Double): ComparisonConfidence = {
    ComparisonConfidence(
      score = cmp(entities, threshold),
      comparison = cmp.operator,
      sourceValue = evaluateInput(cmp.sourceInput, entities.source),
      targetValue = evaluateInput(cmp.targetInput, entities.target)
    )
  }

  private def evaluateInput(inputExec: InputExecution, entity: Entity): Value = inputExec match {
    case ti: TransformInputExecution =>
      val children = ti.inputExecutions.map(child => evaluateInput(child, entity))
      try {
        val transformed = ti.transformerExecution(ArraySeq.unsafeWrapArray(children.map(_.values).toArray[Seq[String]]))
        TransformedValue(ti.operator, transformed, children)
      } catch {
        case NonFatal(ex) =>
          TransformedValue(ti.operator, Seq.empty, children, Some(ex))
      }

    case pi: PathInput => InputValue(pi, pi(entity).values)
  }
}
