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

import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.entity.Link
import math.log
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities

/**
 * Selects links with the highest Kullback-Leibler divergence as suggested in:
 * Andrew McCallum and Kamal Nigam: Employing EM and Pool-Based Active Learning for Text Classification
 *
 * @param normalize If set to true, the divergence is also normalized by substracting the divergence to the nearest reference link.
 */
class KullbackLeiblerDivergenceSelector(normalize: Boolean = false) extends LinkSelector {

  override def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
    val proj = projection(rules, referenceEntities)

    val positiveLinks = for((link, entityPair) <- referenceEntities.positive) yield link.update(entities = Some(entityPair))
    val negativeLinks = for((link, entityPair) <- referenceEntities.negative) yield link.update(entities = Some(entityPair))

    val unlabeled = unlabeledLinks.map(proj)
    val positive = positiveLinks.map(proj)
    val negative = negativeLinks.map(proj)

    val rank = ranking(rules, unlabeled, positive, negative)
    val rankedLinks = unlabeled.map(l => l.link.update(confidence = Some(rank(l))))

    rankedLinks.sortBy(-_.confidence.get).take(3)
  }

  private def projection(rules: Seq[WeightedLinkageRule], referenceEntities: ReferenceEntities): (Link => ProjLink) = {
    new KullbackLeiblerDivergence(rules)
  }

  private def ranking(rules: Seq[WeightedLinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]): (ProjLink => Double) = {
    new Ranking(rules, unlabeled, positive, negative)
  }

  private class KullbackLeiblerDivergence(rules: Seq[WeightedLinkageRule]) extends (Link => ProjLink) {
    
    def apply(link: Link): ProjLink = {
      /** The consensus probability that this link is correct */
      val q = rules.map(probability(_, link)).sum / rules.size

      val vector = rules.map(rule => kullbackLeiblerDivergenceProtected(probability(rule, link), q))
      
      new ProjLink(link, vector)
    }

    def kullbackLeiblerDivergenceProtected(p: Double,  q: Double) = {
      if(q == 0.0) kullbackLeiblerDivergence(p, 0.0001)
      else if(q == 1.0) kullbackLeiblerDivergence(p, 0.9999)
      else kullbackLeiblerDivergence(p, q)
    }

    def kullbackLeiblerDivergence(p: Double, q: Double) = {
      val p1 = if(p <= 0.0) 0.0 else p * log(p / q)
      val p2 = if(p >= 1.0) 0.0 else (1 - p) * log((1 - p) / (1 - q))

      (p1 + p2) / log(2)
    }

    private def probability(rule: WeightedLinkageRule, link: Link) = {
      if(rule(link.entities.get) > 0.0) rule.weight else 0.0
    }
  }
  
  private class Ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]) extends (ProjLink => Double) {

    def apply(p: ProjLink): Double = {
      if(normalize)
        (positive ++ negative).map(r => dist(r, p)).min
      else 
        p.vector.sum / p.vector.size
    }

    private def dist(v1: ProjLink, v2: ProjLink) = {
      (v1.vector zip v2.vector).map(p => (p._1 - p._2).abs).sum / v1.vector.size
    }
  }

  private class ProjLink(val link: Link, val vector: Seq[Double])
}









