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

import org.silkframework.entity.{LinkDecision, ReferenceLink}
import org.silkframework.learning.active.{ActiveLearningReferenceData, LinkCandidate}
import org.silkframework.rule.LinkageRule

import scala.util.Random

/**
 * Selects links based on the distance from the closest reference link.
 */
case class OptimizedSelector() extends LinkSelector {

  override def apply(rules: Seq[WeightedLinkageRule], referenceData: ActiveLearningReferenceData)(implicit random: Random): Seq[LinkCandidate] = {
    val evaluatedReferenceLinks = referenceData.referenceLinks.map(EvaluatedReferenceLink.evaluate(_, rules))
    val evaluatedLinkCandidates = referenceData.linkCandidates.map(EvaluatedLinkCandidate.evaluate(_, rules))

    val rankedLinks = evaluatedLinkCandidates.par.map(_.rankLink(evaluatedReferenceLinks))

    rankedLinks.seq.sortBy(-_.confidence.get).take(3)
  }

  /**
    * Evaluate reference link.
    *
    * @param link The reference link itself
    * @param ruleValues The evaluated rules.
    * @param indices The indices of the rules for which the reference link is fulfilled.
    */
  case class EvaluatedReferenceLink(link: ReferenceLink, ruleValues: IndexedSeq[Double], indices: Seq[Int]) {

    def distance(linkCandidate: EvaluatedLinkCandidate): Double = {
      var dist = 0.0
      for(i <- indices) {
        val diff = linkCandidate.ruleValues(i) - ruleValues(i)
        dist += diff * diff
      }
      dist / indices.size
    }

  }

  object EvaluatedReferenceLink {

    def evaluate(link: ReferenceLink, rules: Seq[LinkageRule]): EvaluatedReferenceLink = {
      val values = rules.map(rule => rule(link.linkEntities, limit = -1.0)).toIndexedSeq
      val indices = link.decision match {
        case LinkDecision.POSITIVE =>
          for((value, index) <- values.zipWithIndex if value > 0.0) yield index
        case LinkDecision.NEGATIVE =>
          for ((value, index) <- values.zipWithIndex if value <= 0.0) yield index
        case _ =>
          Seq.empty
      }
      EvaluatedReferenceLink(link, values, indices)
    }
  }

  /**
    * Evaluate link candidate.
    *
    * @param link       The reference link itself
    * @param ruleValues The evaluated rules.
    */
  case class EvaluatedLinkCandidate(link: LinkCandidate, ruleValues: IndexedSeq[Double]) {

    /**
      * Ranks a link by updating its confidence to the distance from the closest reference link.
      */
    def rankLink(referenceLinks: Seq[EvaluatedReferenceLink]): LinkCandidate = {
      val distancesToReferenceLinks = referenceLinks.map(_.distance(this)).min
      link.withConfidence(distancesToReferenceLinks)
    }

  }

  object EvaluatedLinkCandidate {
    def evaluate(link: LinkCandidate, rules: Seq[LinkageRule]): EvaluatedLinkCandidate = {
      val values = rules.map(rule => rule(link.linkEntities, limit = -1.0)).toIndexedSeq
      EvaluatedLinkCandidate(link, values)
    }
  }

}









