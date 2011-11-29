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
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

//TODO rename?
class UncertaintySelector() extends LinkSelector {

  override def projection(rules: Seq[LinkageRule], referenceEntities: ReferenceEntities): (Link => ProjLink) = {
    new Projection(rules)
  }

  override def ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]): (ProjLink => Double) = {
    new Ranking(rules, unlabeled, positive, negative)
  }

  private class Projection(rules: Seq[LinkageRule]) extends (Link => ProjLink) {
    def apply(link: Link): ProjLink = {
      ProjLink(link, rules.map(rule => rule(link.entities.get)))
    }
  }

  private class Ranking(rules: Seq[LinkageRule], unlabeled: Traversable[ProjLink], positive: Traversable[ProjLink], negative: Traversable[ProjLink]) extends (ProjLink => Double) {
    def apply(p: ProjLink): Double = {
      (positive ++ negative).map(distance(_, p)).min
    }

    private def distance(v1: ProjLink, v2: ProjLink) = {
      sqrt((v1.vector zip v2.vector).map(p => pow(p._1 - p._2, 2.0)).sum) / (2.0 * v1.vector.size)
    }
  }
}









