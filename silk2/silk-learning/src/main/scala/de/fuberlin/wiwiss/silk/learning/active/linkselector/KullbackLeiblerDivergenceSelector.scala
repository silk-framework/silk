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
import math.{pow, sqrt, log}
import java.io.{FileWriter, BufferedWriter}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities

class KullbackLeiblerDivergenceSelector extends LinkSelector {

  override def projection(rules: Seq[LinkageRule], referenceEntities: ReferenceEntities): (Link => ProjLink) = {
    new KullbackLeiblerDivergence(rules.map(rule => new ProbLinkageRule(rule, referenceEntities)).filter(_.isDefined))
  }

  override def ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]): (ProjLink => Double) = {
    new Ranking(rules, unlabeled, positive, negative)
  }

  private class KullbackLeiblerDivergence(rules: Seq[ProbLinkageRule]) extends (Link => ProjLink) {
    
    def apply(link: Link): ProjLink = {
      /** The consensus probability that this link is correct */
      val q = rules.map(probability(_, link)).sum / rules.size

      //rank(ref) == 0
      //rank(link) == r

      //projection: for each rule: distance from next reference link which it fulfills
      //example: rule1(ref1) == 0, rule1(ref2) == 1, rule1(link)==0 => dist == 0

      val vector = 
        if(q == 0.0 || q == 1.0)
          rules.map(rule => 0.0)
        else
          rules.map(rule => kullbackLeiblerDivergence(q, rule, link))
      
      new ProjLink(link, vector)
    }

    def kullbackLeiblerDivergence(q: Double, rule: ProbLinkageRule, link: Link) = {
      /** The probability that this link is true based on the given linkage rule */
      val p = probability(rule, link)

      val p1 = if(p <= 0.0) 0.0 else p * log(p / q)
      val p2 = if(p >= 1.0) 0.0 else (1 - p) * log((1 - p) / (1 - q))

      (p1 + p2) / log(2)
    }

    private def probability(rule: ProbLinkageRule, link: Link) = {
      rule(link)
    }
  }
  
  private class Ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]) extends (ProjLink => Double) {

//    val means = Array.fill(rules.size)(0.0)
//    for(i <- 0 until rules.size) {
//      means(i) = unlabeled.map(_.vector(i)).sum / unlabeled.size
//    }
//
//    val variances = Array.fill(rules.size)(0.0)
//    for(i <- 0 until rules.size) {
//      variances(i) = unlabeled.map(p => pow(p.vector(i) - means(i), 2.0)).sum
//    }
//
//    val maxIndex = (variances.zipWithIndex).maxBy(_._1)._2

    def apply(p: ProjLink): Double = {
      //val informationGain = (positive ++ negative).map(r => (r.vector(maxIndex) - p.vector(maxIndex)).abs).min
      //val informationGain = (positivePoints ++ negativePoints).map(r => distance(r, p)).sum
      //val informationGain = (positive ++ negative).map(r => meanDistance(r, p)).min

      p.vector.sum / p.vector.size
    }

    private def distance(v1: ProjLink, v2: ProjLink) = {
      sqrt((v1.vector zip v2.vector).map(p => pow(p._1 - p._2, 2.0)).sum) / (2.0 * v1.vector.size)
    }

    private def maxDistance(v1: ProjLink, v2: ProjLink) = {
      (v1.vector zip v2.vector).map(p => (p._1 - p._2).abs).max
    }

    private def meanDistance(v1: ProjLink, v2: ProjLink) = {
      (v1.vector zip v2.vector).map(p => (p._1 - p._2).abs).sum / v1.vector.size
    }
  }
}









