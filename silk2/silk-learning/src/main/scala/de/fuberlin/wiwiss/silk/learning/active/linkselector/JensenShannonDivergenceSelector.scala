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

import math.log
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.entity.{Entity, Link}
import de.fuberlin.wiwiss.silk.util.DPair
import math.min

/**
 * Selects links based on the Jensen-Shannon divergence from the closest reference link.
 */
case class JensenShannonDivergenceSelector(fulfilledOnly: Boolean = true) extends LinkSelector {

  override def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
    val rankedLinks = unlabeledLinks.par.map(link => link.update(confidence = Some(rateLink(link, rules, referenceEntities))))

    rankedLinks.seq.sortBy(-_.confidence.get).take(3)
  }
  
  def rateLink(link: Link, rules: Seq[WeightedLinkageRule], referenceEntities: ReferenceEntities): Double = {
    val posDist = referenceEntities.positive.values.map(referencePair => distanceToReferenceLink(link, rules, referencePair, true)).min
    val negDist = referenceEntities.negative.values.map(referencePair => distanceToReferenceLink(link, rules, referencePair, false)).min
    
    min(posDist, negDist)
  }

  def distanceToReferenceLink(link: Link, rules: Seq[WeightedLinkageRule], referencePair: DPair[Entity], isPos: Boolean) =  {
    val fulfilledRules = 
      if (fulfilledOnly) {
        if(isPos) rules.filter(rule => rule(referencePair) > 0.0) else rules.filter(rule => rule(referencePair) <= 0.0)
      }
      else
        rules

    val q = project(fulfilledRules, link.entities.get)
    
    val p = project(fulfilledRules, referencePair)

    jensenShannonDivergence(p, q) + 0.5 * entropy(q)
  }

  private def project(rules: Seq[WeightedLinkageRule], entityPair: DPair[Entity]) = {
    rules.map(rule => probability(rule, entityPair)).sum / rules.size
  }

  private def probability(rule: WeightedLinkageRule, entityPair: DPair[Entity]) = {
    rule(entityPair) * 0.5 + 0.5
  }

  private def jensenShannonDivergence(p1: Double, p2: Double) = {
    entropy(0.5 * (p1 + p2)) - 0.5 * (entropy(p1) + entropy(p2))
  }

  private def entropy(p: Double) = {
    if(p <= 0.0 || p >= 1.0)
      0.0
    else
      (-p * log(p) - (1 - p) * log(1 - p)) / log(2)
  }
}









