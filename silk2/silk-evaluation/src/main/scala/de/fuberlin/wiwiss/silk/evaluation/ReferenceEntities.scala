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

package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.entity.{Link, Entity}

/**
 * Holds the entities which correspond to a set of reference links.
 */
case class ReferenceEntities(positive: Map[Link, DPair[Entity]] = Map.empty,
                             negative: Map[Link, DPair[Entity]] = Map.empty) {

  /** True, if no entities are available */
  def isEmpty = positive.isEmpty && negative.isEmpty

  /** True, if positive and negative entities are available */
  def isDefined = !positive.isEmpty && !negative.isEmpty

  def withPositive(entityPair: DPair[Entity]) = {
    copy(positive = positive + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }

  def withNegative(entityPair: DPair[Entity]) = {
    copy(negative = negative + (new Link(entityPair.source.uri, entityPair.target.uri) -> entityPair))
  }
}

object ReferenceEntities {
  def empty = ReferenceEntities(Map.empty, Map.empty)

  def fromEntities(positiveEntities: Traversable[DPair[Entity]], negativeEntities: Traversable[DPair[Entity]]) = {
    ReferenceEntities(
      positive = positiveEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap,
      negative = negativeEntities.map(i => (new Link(i.source.uri, i.target.uri), i)).toMap
    )
  }
}