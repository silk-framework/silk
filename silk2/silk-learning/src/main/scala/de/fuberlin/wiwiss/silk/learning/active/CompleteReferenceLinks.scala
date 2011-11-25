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

package de.fuberlin.wiwiss.silk.learning.active

import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceEntities, ReferenceLinks}
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

object CompleteReferenceLinks {
  def apply(referenceEntities: ReferenceEntities, unlabeledLinks: Traversable[Link], population: Population) = {
    implicit lazy val rules = population.individuals.map(_.node.build)

    val positive = {
      if(!referenceEntities.positive.isEmpty) {
        referenceEntities.positive
      } else {
        val maxLink = unlabeledLinks.map(withAgreement).maxBy(_.confidence)
        Map(maxLink -> maxLink.entities.get)
      }
    }

    val negative = {
      if(!referenceEntities.negative.isEmpty) {
        referenceEntities.negative
      } else {
        val minLink = unlabeledLinks.map(withAgreement).minBy(_.confidence)
        Map(minLink -> minLink.entities.get)
      }
    }

    ReferenceEntities(positive, negative)
  }

  def withAgreement(link: Link)(implicit rules: Traversable[LinkageRule]) = {
    val confidenceSum = rules.map(_.apply(link.entities.get)).sum
    val confidence = confidenceSum / rules.size

    link.update(confidence = Some(confidence))
  }
}