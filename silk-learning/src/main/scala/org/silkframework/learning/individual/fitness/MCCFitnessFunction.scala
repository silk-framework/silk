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

package org.silkframework.learning.individual.fitness

import org.silkframework.rule.LinkageRule
import org.silkframework.rule.similarity.{Comparison, Aggregation, SimilarityOperator}
import org.silkframework.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import math.max

case class MCCFitnessFunction(sizePenalty: Double = 0.005) extends FitnessFunction {

  def apply(referenceEntities: ReferenceEntities) = { (linkageRule: LinkageRule) =>
    val score = LinkageRuleEvaluator(linkageRule, referenceEntities).score
    val penalty = linkageRule.operator.map(countComparisons).getOrElse(0) * sizePenalty

    max(score - penalty, 0.0)
  }

  private def countComparisons(op: SimilarityOperator): Int = op match {
    case agg: Aggregation => agg.operators.map(countComparisons).sum
    case cmp: Comparison => 1
  }
}