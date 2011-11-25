/*
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.learning.active.linkselector

import de.fuberlin.wiwiss.silk.entity.Link
import math.log

class EntropySelector(rules: Seq[WeightedLinkageRule], unlabeledLinks: Seq[Link]) extends LinkSelector {
  def apply(): Seq[Link] = {
    val valLinks = for(link <- unlabeledLinks) yield link.update(confidence = Some(entropy(link)))
    valLinks.sortBy(-_.confidence.get).take(3)
  }

  def entropy(link: Link) = {
    val fulfilledRules = rules.filter(rule => rule(link.entities.get) > 0.0)
    val p = fulfilledRules.size.toDouble / rules.size

    (-p * log(p) - (1 - p) * log(1 - p)) / log(2)
  }
}









