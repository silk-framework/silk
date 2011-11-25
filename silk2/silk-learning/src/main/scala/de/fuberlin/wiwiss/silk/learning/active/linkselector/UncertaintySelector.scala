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

import de.fuberlin.wiwiss.silk.entity.Link
import math.{pow, sqrt}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities

class UncertaintySelector(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities) {

  /** Each positive link defines a point in the space spanned by the linkage rules. */
  val positivePoints: Traversable[Seq[Double]] = {
    for((link, entityPair) <- referenceEntities.positive) yield {
      rules.map(_.apply(entityPair))
    }
  }

  /** Each negative link defines a point in the space spanned by the linkage rules. */
  val negativePoints: Traversable[Seq[Double]] = {
    for((link, entityPair) <- referenceEntities.negative) yield {
      rules.map(_.apply(entityPair))
    }
  }

  def apply(): Seq[Link] = {
    val valLinks = for(link <- unlabeledLinks) yield link.update(confidence = Some(uncertainty(link)))
    valLinks.sortBy(_.confidence.get.abs).take(3)
  }

  def uncertainty(link: Link) = {
    val c = rules.map(rule => rule(link.entities.get))

    val posDist = positivePoints.map(distance(_, c)).min
    val negDist = negativePoints.map(distance(_, c)).min

    (negDist - posDist) / (posDist + negDist)
  }

  def distance(v1: Seq[Double], v2: Seq[Double]) = {
    sqrt((v1 zip v2).map(p => pow(p._1 - p._2, 2.0)).sum) / (2.0 * rules.size)
  }
}









