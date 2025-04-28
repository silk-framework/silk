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
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.runtime.validation.ValidationIssue
import org.silkframework.util.{DPair, Identifier}

import scala.xml.Node

/**
 * An aggregation combines multiple similarity values into a single value.
 */
case class Aggregation(id: Identifier = Operator.generateId,
                       weight: Int = 1,
                       aggregator: Aggregator,
                       operators: Seq[SimilarityOperator]) extends SimilarityOperator {

  require(weight > 0, "weight > 0")
  //TODO learning currently may produce empty aggregations when cleaning
  //require(!operators.isEmpty, "!operators.isEmpty")

  def indexing: Boolean = operators.exists(_.indexing)

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit The similarity threshold.
   * @return The similarity as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  override def apply(entities: DPair[Entity], limit: Double): Option[Double] = {
    aggregator(operators, entities, limit).score
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param threshold The similarity threshold.
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  override def index(entity: Entity, sourceOrTarget: Boolean, threshold: Double): Index = {
    val indexSets = {
      for (op <- operators if op.indexing) yield {
        val index = op.index(entity, sourceOrTarget, threshold)
        index
      }
    }

    aggregator.aggregateIndexes(indexSets)
  }

  override def validate(): Seq[ValidationIssue] = {
    operators.flatMap(_.validate())
  }

  override def children: Seq[SimilarityOperator] = operators

  override def withChildren(newChildren: Seq[Operator]): Aggregation = {
    copy(operators = newChildren.map(_.asInstanceOf[SimilarityOperator]))
  }

  override def withContext(taskContext: TaskContext): Aggregation = {
    copy(operators = operators.map(_.withContext(taskContext)))
  }
}

object Aggregation {

  /**
   * XML serialization format.
   */
  implicit object AggregationFormat extends XmlFormat[Aggregation] {

    import XmlSerialization._

    def read(node: Node)(implicit readContext: ReadContext): Aggregation = {
      val weightStr = (node \ "@weight").text
      val aggregator = Aggregator((node \ "@type").text, Operator.readParams(node))

      Aggregation(
        id = Operator.readId(node),
        weight = if (weightStr.isEmpty) 1 else weightStr.toInt,
        operators = node.child.filter(n => n.label == "Aggregate" || n.label == "Compare").map(fromXml[SimilarityOperator]).toSeq,
        aggregator = aggregator
      )
    }

    def write(value: Aggregation)(implicit writeContext: WriteContext[Node]): Node = {
      value.aggregator match {
        case Aggregator(plugin, params) =>
          <Aggregate id={value.id} weight={value.weight.toString} type={plugin.id}>
            {value.operators.map(toXml[SimilarityOperator])}
          </Aggregate>
      }
    }
  }
}
