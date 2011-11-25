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

package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkagerule.{Operator, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.input.{PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{SimilarityOperator, Comparison, Aggregation}
import math.max

class FitnessFunction(referenceEntities: ReferenceEntities,
                      unlabeledLinks: Traversable[Link]) extends (LinkageRule => Double) {

  def apply(linkageRule: LinkageRule) = {
    val score = LinkageRuleEvaluator(linkageRule, referenceEntities).score

    val penalty = linkageRule.operator.map(countComparisons).getOrElse(0) / 300.0

    max(score - penalty, 0.0)
  }

  private def countComparisons(op: SimilarityOperator): Int = op match {
    case agg: Aggregation => agg.operators.map(countComparisons).sum
    case cmp: Comparison => 1
  }

//  private def filter(linkageRule: LinkageRule) = {
//    val entityPairs = unlabeledLinks.toSeq.map(_.entities.get)
//    val shuffledEntityPairs = for((s, t) <- entityPairs.map(_.source) zip (entityPairs.tail.map(_.target) :+ entityPairs.head.target)) yield DPair(s, t)
//
//    val count = (entityPairs ++ shuffledEntityPairs).filter(linkageRule(_) > 0.0).size
//
//    count > 0 && count <= unlabeledLinks.size
//  }
}