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
import math.{pow, sqrt, abs, log}
import org.silkframework.evaluation.ReferenceEntities
import org.silkframework.rule.LinkageRule

/**
 * Link Selector which distributes the links uniformly.
 */
class UniformSelector() extends LinkSelector {

  def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
    val proj = projection(rules, referenceEntities)

    val positiveLinks = for((link, entityPair) <- referenceEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeLinks = for((link, entityPair) <- referenceEntities.negative) yield link.update(entities = Some(entityPair))

    val unlabeled = unlabeledLinks.map(proj)
    val positive = positiveLinks.map(proj)
    val negative = negativeLinks.map(proj)

    val rank = ranking(rules, unlabeled, positive, negative)
    val rankedLinks = unlabeled.par.map(l => l.link.update(confidence = Some(rank(l))))

    rankedLinks.seq.sortBy(-_.confidence.get).take(3)
  }

  private def projection(rules: Seq[LinkageRule], referenceEntities: ReferenceEntities): (Link => ProjLink) = {
    new Projection(rules)
  }

  private def ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]): (ProjLink => Double) = {
    new Ranking(rules, unlabeled, positive, negative)
  }

  private class Projection(rules: Seq[LinkageRule]) extends (Link => ProjLink) {
    def apply(link: Link): ProjLink = {
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

  private class ProjLink(val link: Link, val vector: Seq[Double])
}









