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

package de.fuberlin.wiwiss.silk.learning.active.linkselector

import de.fuberlin.wiwiss.silk.learning.individual.Individual
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import math.max
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.entity.Link

class WeightedLinkageRule(individual: Individual) extends LinkageRule(individual.node.build.operator) {
  /** The weight of this linkage rule. Never smaller than 0.0001 */
  val weight = max(0.0001, individual.fitness)
}

class ProbLinkageRule(rule: LinkageRule, referenceEntities: ReferenceEntities) {
  
  val posPoints = referenceEntities.positive.values.map(rule(_)).filter(_ > 0.0)

  val negPoints = referenceEntities.negative.values.map(rule(_)).filter(_ <= 0.0)

  def isDefined = !posPoints.isEmpty && !negPoints.isEmpty

  def apply(link: Link): Double = {
    val p = rule(link.entities.get)

    val posDist = posPoints.map(p - _).map(_.abs).min
    val negDist = negPoints.map(p - _).map(_.abs).min

    if(posDist == 0.0 && negDist == 0.0)
      0.5
    else
      (negDist - posDist) / (posDist + negDist) * 0.5 + 0.5
  }
}









