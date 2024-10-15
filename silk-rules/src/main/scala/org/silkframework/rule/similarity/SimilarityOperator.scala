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

package org.silkframework.rule.similarity

import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.{Operator, TaskContext}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationIssue
import org.silkframework.util.DPair

import scala.xml.Node

/**
 * An operator which computes the similarity between two entities.
 * Base class of aggregations and comparisons.
 */
trait SimilarityOperator extends Operator {

  def weight: Int

  def indexing: Boolean

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit Only returns values if the confidence is higher than the limit
    * @return The confidence as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  def apply(entities: DPair[Entity], limit: Double = 0.0): Option[Double]

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param limit The confidence limit.
    * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  def index(entity: Entity, sourceOrTarget: Boolean, limit: Double): Index

  /**
    * Validates this rule operator.
    * This should cover non-fatal issues that should be fixed by the user after rule creation.
    * Issues that lead to an inconsistent and unusable rule should not be checked here, but instead throw an exception in the constructor.
    */
  def validate(): Seq[ValidationIssue] = {
    Seq.empty
  }

  override def withContext(taskContext: TaskContext): SimilarityOperator = {
    this
  }
}

object SimilarityOperator {

  /**
   * XML serialization format.
   */
  implicit object SimilarityOperatorFormat extends XmlFormat[SimilarityOperator] {


    def read(node: Node)(implicit readContext: ReadContext): SimilarityOperator = {
      node match {
        case node@ <Aggregate>{_*}</Aggregate> => fromXml[Aggregation](node)
        case node@ <Compare>{_*}</Compare> => fromXml[Comparison](node)
      }
    }

    def write(value: SimilarityOperator)(implicit writeContext: WriteContext[Node]): Node = {
      value match {
        case c: Comparison => toXml(c)
        case a: Aggregation => toXml(a)
      }
    }
  }
}
