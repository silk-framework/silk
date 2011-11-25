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

class KullbackLeiblerDivergenceSelector(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities) {

  /** Each positive link defines a point in the space spanned by the linkage rules. */
  val positivePoints: Traversable[Seq[Double]] = {
    for((link, entityPair) <- referenceEntities.positive) yield {
      kullbackLeiblerDivergenceVector(link.update(entities = Some(entityPair)))
    }
  }

  /** Each negative link defines a point in the space spanned by the linkage rules. */
  val negativePoints: Traversable[Seq[Double]] = {
    for((link, entityPair) <- referenceEntities.negative) yield {
      kullbackLeiblerDivergenceVector(link.update(entities = Some(entityPair)))
    }
  }

  def apply(): Seq[Link] = {
    val valLinks = for(link <- unlabeledLinks) yield link.update(confidence = Some(informationGain(link)))
    valLinks.sortBy(-_.confidence.get).take(3)
  }

  private def informationGain(link: Link) = {
    val pl = kullbackLeiblerDivergenceVector(link)
    (positivePoints ++ negativePoints).map(p => distance(p, pl)).min
  }

  write("space_plain.csv", plainVector)
  write("space_kl.csv", kullbackLeiblerDivergenceVector)

  private def write(file: String,  proj: (Link => Seq[Double])) {
    val writer = new BufferedWriter(new FileWriter(file))

    for(link <- unlabeledLinks) {
      val entities = link.entities.get
      val l0 = entities.source.evaluate(0) == entities.target.evaluate(0)
      val l1 = entities.source.evaluate(1) == entities.target.evaluate(1)
      writer.write((link.source + link.target).replace(",", "%2C") + "," + l0 + "_"  + l1 + "," + proj(link).mkString(",") + "\n")
    }

    val posLinks = for((link, entities) <- referenceEntities.positive) yield link.update(entities = Some(entities))
    for(link <- posLinks) {
      writer.write((link.source + link.target).replace(",", "%2C") + ",pos," + proj(link).mkString(",") + "\n")
    }

    val negLinks = for((link, entities) <- referenceEntities.negative) yield link.update(entities = Some(entities))
    for(link <- negLinks) {
      writer.write((link.source + link.target).replace(",", "%2C") + ",neg," + proj(link).mkString(",") + "\n")
    }

    writer.close()
  }

  def plainVector(link: Link): Seq[Double] = {
    rules.map(rule => rule(link.entities.get, 0.0) * 0.5 + 0.5)
  }

  def kullbackLeiblerDivergenceVector(link: Link): Seq[Double] = {
    /** The consensus probability that this link is correct */
    val q = rules.map(probability(_, link)).sum / rules.size
    //val q = probability(weightedRules.maxBy(_.weight), link)

    if(q == 0.0 || q == 1.0)
      rules.map(rule => 0.0)
    else
      rules.map(rule => kullbackLeiblerDivergence(q, rule, link))
  }

  def kullbackLeiblerDivergence(q: Double, rule: LinkageRule, link: Link) = {
    /** The probability that this link is true based on the given linkage rule */
    val p = probability(rule, link)

    val p1 = if(p <= 0.0) 0.0 else p * log(p / q)
    val p2 = if(p >= 1.0) 0.0 else (1 - p) * log((1 - p) / (1 - q))

    (p1 + p2) / log(2)
  }

  //TODO include fitness
  private def probability(rule: LinkageRule, link: Link) = {
    //rule(link.entities.get, 0.0) * 0.5 + 0.5
    if(rule(link.entities.get, 0.0) > 0.0) 1.0 else 0.0
  }

  def distance(v1: Seq[Double], v2: Seq[Double]) = {
    sqrt((v1 zip v2).map(p => pow(p._1 - p._2, 2.0)).sum) / (2.0 * rules.size)
  }
}









