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
import util.Random

/**
 * Link Selector which selects a random link.
 * This can be used as a baseline against other selectors can be compared.
 */
case class RandomSelector() extends LinkSelector {
  override def apply(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link], referenceEntities: ReferenceEntities): Seq[Link] = {
    Seq(unlabeledLinks(Random.nextInt(unlabeledLinks.size)))
  }
}