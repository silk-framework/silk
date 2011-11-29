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
import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

trait LinkSelector {
  def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
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

  def projection(rules: Seq[LinkageRule], referenceEntities: ReferenceEntities): (Link => ProjLink)

  def ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]): (ProjLink => Double)
}









