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

import org.silkframework.learning.active.{ActiveLearningReferenceData, LinkCandidate}
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.evaluation.ReferenceEntities

import scala.math.log
import scala.util.Random

/**
 * Link Selector which distributes the links uniformly.
 */
case class UniformSelector() extends LinkSelector {

  def apply(rules: Seq[WeightedLinkageRule], referenceData: ActiveLearningReferenceData)(implicit random: Random): Seq[LinkCandidate] = {
    val referenceEntities = referenceData.toReferenceEntities
    val proj = projection(rules, referenceEntities)

    val positiveLinks = referenceEntities.positiveEntities map LinkSelectorHelper.pairToLink
    val negativeLinks = referenceEntities.negativeEntities map LinkSelectorHelper.pairToLink

    val unlabeled = referenceData.linkCandidates.map(proj)
    val positive = positiveLinks.map(LinkCandidate.fromLink).map(proj)
    val negative = negativeLinks.map(LinkCandidate.fromLink).map(proj)

    val rank = ranking(rules, unlabeled, positive, negative)
    val rankedLinks = unlabeled.par.map(l => l.link.withConfidence(rank(l)))

    rankedLinks.seq.sortBy(-_.confidence.get).take(3)
  }

  private def projection(rules: Seq[LinkageRule], referenceEntities: ReferenceEntities): (LinkCandidate => ProjLink) = {
    new Projection(rules)
  }

  private def ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]): (ProjLink => Double) = {
    new Ranking(rules, unlabeled, positive, negative)
  }

  private class Projection(rules: Seq[LinkageRule]) extends (LinkCandidate => ProjLink) {
    def apply(link: LinkCandidate): ProjLink = {
      new ProjLink(link, rules.map(rule => rule(link.entities.get) * 0.5 + 0.5))
    }
  }

  private class Ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]) extends (ProjLink => Double) {
    def apply(p: ProjLink): Double = {
      (positive ++ negative).map(distance(_, p)).min
    }

    private def distance(v1: ProjLink, v2: ProjLink) = {
      (v1.vector zip v2.vector).map(p => jensenShannonDivergence(p._1,p._2)).sum / (2.0 * v1.vector.size)
    }
    
    private def jensenShannonDivergence(p1: Double, p2: Double) = {
      //(p1 - p2).abs * entropy((p1 + p2) * 0.5)
      entropy(0.5 * (p1 + p2)) - 0.5 * (entropy(p1) + entropy(p2)) + entropy(p2)
    }
  
    private def entropy(p: Double) = {
      require(p >= 0.0 && p <= 1.0)
      if(p <= 0.0 || p >= 1.0)
        0.0
      else
        (-p * log(p) - (1 - p) * log(1 - p)) / log(2)
    }
  }

  private class ProjLink(val link: LinkCandidate, val vector: Seq[Double])
}









