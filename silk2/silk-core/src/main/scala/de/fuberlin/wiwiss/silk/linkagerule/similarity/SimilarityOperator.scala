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

package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import de.fuberlin.wiwiss.silk.entity.{Index, Entity}
import de.fuberlin.wiwiss.silk.util.DPair

/**
 * An operator which computes the similarity between two entities.
 * Base class of aggregations and comparisons.
 */
trait SimilarityOperator extends Operator {
  val required: Boolean

  val weight: Int

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit Only returns values if the confidence is higher than the limit
   *
   * @return The confidence as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double]

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param limit The confidence limit.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  def index(entity: Entity, limit: Double): Index

  /**
   * Serializes this operator as XML.
   */
  def toXML(implicit prefixes: Prefixes): Node
}

object SimilarityOperator {
  def fromXML(nodes: Seq[Node])(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Seq[SimilarityOperator] = {
    nodes.collect {
      case node@ <Aggregate>{_*}</Aggregate> => Aggregation.fromXML(node)
      case node@ <Compare>{_*}</Compare> => Comparison.fromXML(node)
    }
  }
}
