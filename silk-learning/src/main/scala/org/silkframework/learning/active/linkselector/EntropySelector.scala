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

import org.silkframework.entity.Link
import org.silkframework.learning.active.{ActiveLearningReferenceData, LinkCandidate}

import scala.math.log
import scala.util.Random

/**
 * Link Selector which selects the links with the highest vote entropy.
 */
case class EntropySelector() extends LinkSelector {

  override def apply(rules: Seq[WeightedLinkageRule], referenceData: ActiveLearningReferenceData)(implicit random: Random): Seq[LinkCandidate] = {
    val maxLink = referenceData.linkCandidates.par.maxBy(link => entropy(rules, link))
    Seq(maxLink)
  }

  private def entropy(rules: Seq[WeightedLinkageRule], link: Link) = {
    val fulfilledRules = rules.count(rule => rule(link.entities.get) > 0.0)
    val p = fulfilledRules.toDouble / rules.size

    if(p == 0.0 || p == 1.0)
      0.0
    else
      (-p * log(p) - (1 - p) * log(1 - p)) / log(2)
  }
}









