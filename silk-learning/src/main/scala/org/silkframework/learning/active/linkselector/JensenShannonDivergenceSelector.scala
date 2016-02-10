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

package org.silkframework.learning.active.linkselector

import org.silkframework.entity.{Entity, Link}
import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.rule.LinkageRule
import org.silkframework.util.DPair

import scala.math.log

/**
 * Selects links based on the Jensen-Shannon divergence from the closest reference link.
 */
case class JensenShannonDivergenceSelector(fulfilledOnly: Boolean = true) extends LinkSelector {
  /**
   * Returns the links with the highest Jensen-Shannon divergence from any reference link.
   */
  override def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
    val posDist = referenceEntities.positiveEntities.map(referencePair => new ReferenceLinkDistance(referencePair, rules, true))
    val negDist = referenceEntities.negativeEntities.map(referencePair => new ReferenceLinkDistance(referencePair, rules, false))
    val dist = posDist ++ negDist

    val rankedLinks = unlabeledLinks.par.map(rankLink(dist))

    rankedLinks.seq.sortBy(-_.confidence.get).take(3)
  }

  /**
   * Ranks a link by updating its confidence to the distance from the closes reference link.
   */
  def rankLink(dist: Traversable[ReferenceLinkDistance])(link: Link): Link = {
    val minDist = dist.map(_(link)).min
    link.update(confidence = Some(minDist))
  }

  /**
   * Computes the Jensen-Shannon divergence from a specific reference link.
   */
  private class ReferenceLinkDistance(entities: DPair[Entity], rules: Seq[LinkageRule], isPos: Boolean) {
    /**
     * Returns the Jensen-Shannon divergence from a reference link.
     */
    def apply(link: Link) = {
      val qLink = q(link)
      jensenShannonDivergence(p, qLink) + 0.5 * entropy(qLink)
    }
    
    /** All linkage rules which fulfill this reference link */
    private val fulfilledRules = {
      if (fulfilledOnly) {
        if(isPos) rules.filter(rule => rule(entities) > 0.0) else rules.filter(rule => rule(entities) <= 0.0)
      }
      else
        rules
    }
    
    private val p = project(fulfilledRules, entities)
    
    private def q(link: Link) = project(fulfilledRules, link.entities.get)
    
    private def project(rules: Seq[LinkageRule], entityPair: DPair[Entity]) = {
      rules.map(rule => probability(rule, entityPair)).sum / rules.size
    }
  
    private def probability(rule: LinkageRule, entityPair: DPair[Entity]) = {
      rule(entityPair) * 0.5 + 0.5
    }

    /**
     * Computes the Jensen-Shannon divergence between two binary variables.
     */
    private def jensenShannonDivergence(p1: Double, p2: Double) = {
      entropy(0.5 * (p1 + p2)) - 0.5 * (entropy(p1) + entropy(p2))
    }

    /**
     * Computes the binary entropy.
     */
    private def entropy(p: Double) = {
      if(p <= 0.0 || p >= 1.0)
        0.0
      else
        (-p * log(p) - (1 - p) * log(1 - p)) / log(2)
    }
  }
}









