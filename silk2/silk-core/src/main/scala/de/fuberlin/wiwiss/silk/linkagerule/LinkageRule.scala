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

package de.fuberlin.wiwiss.silk.linkagerule

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.config.Prefixes
import evaluation.{DetailedEvaluator, DetailedIndexer}
import math.abs
import similarity.SimilarityOperator
import xml.Node
import de.fuberlin.wiwiss.silk.entity.{Index, Entity}
import de.fuberlin.wiwiss.silk.util.XMLUtils._

/**
 * A Linkage Rule specifies the conditions which must hold true so that a link is generated between two entities.
 */
case class LinkageRule(operator: Option[SimilarityOperator] = None) {
  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit If the confidence is below this limit, it will be capped to -1.0.
   *
   * @return The confidence as a value between -1.0 and 1.0.
   *         -1.0 for definitive non-matches.
   *         +1.0 for definitive matches.
   */
  def apply(entities: DPair[Entity], limit: Double = 0.0): Double = {

    println(DetailedEvaluator(this, entities, limit).get.toXML.toFormattedString)

    operator match {
      case Some(op) => op(entities, limit).getOrElse(-1.0)
      case None => -1.0
    }
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param limit The confidence limit
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  def index(entity: Entity, limit: Double = 0.0): Index = {

    println(DetailedIndexer(this, entity, limit).toXML.toFormattedString)

    operator match {
      case Some(op) => op.index(entity, limit)
      case None => Index.empty
    }
  }

  /**
   * Serializes this Link Condition as XML.
   */
  def toXML(implicit prefixes: Prefixes = Prefixes.empty) = {
    <LinkageRule>
      {operator.toList.map(_.toXML)}
    </LinkageRule>
  }
}

/**
 * Creates new linkage rules.
 */
object LinkageRule {
  /**
   * Creates a new linkage rule with one root operator.
   */
  def apply(operator: SimilarityOperator): LinkageRule = LinkageRule(Some(operator))

  /**
   * Reads a linkage rule from xml.
   */
  def fromXML(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]) = {
    LinkageRule(SimilarityOperator.fromXML(node.child).headOption)
  }
}
