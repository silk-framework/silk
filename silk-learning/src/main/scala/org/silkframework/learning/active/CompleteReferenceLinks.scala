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

package org.silkframework.learning.active

import org.silkframework.entity.LinkDecision
import org.silkframework.learning.individual.Population

/**
 * Completes a set of reference links i.e. makes sure that it contains at least one positive and one negative link.
 * If positive links and/or negative links are missing, links from the unlabeled pool are added to the reference links.
 */
object CompleteReferenceLinks {

  /** Maximum number of unlabeled links to be evaluated for addition */
  private val maxLinks = 50

  /** Maximum number of rules of the population used to evaluate a link */
  private val maxRules = 10

  /**
   * Completes a set of reference links.
   */
  def apply(referenceData: ActiveLearningReferenceData, population: Population): ActiveLearningReferenceData = {
    /** The unlabeled links where the confidence for each link is set to the average confidence amongst all rules */
    lazy val linksWithConfidence = {
      val rules = population.individuals.take(maxRules).map(_.node.build)

      for(link <- referenceData.linkCandidates.take(maxLinks)) yield {
        val confidenceSum = rules.map(_(link.entities.get, -1.0)).sum
        val confidence = confidenceSum / rules.size

        link.update(confidence = Some(confidence))
      }
    }

    var referenceLinks = referenceData.referenceLinks
    if(!referenceLinks.exists(_.decision == LinkDecision.POSITIVE)) {
      referenceLinks = linksWithConfidence.maxBy(_.confidence) +: referenceLinks
    }
    if (!referenceLinks.exists(_.decision == LinkDecision.NEGATIVE)) {
      referenceLinks = linksWithConfidence.minBy(_.confidence) +: referenceLinks
    }
    referenceData.copy(referenceLinks = referenceLinks)
  }
}