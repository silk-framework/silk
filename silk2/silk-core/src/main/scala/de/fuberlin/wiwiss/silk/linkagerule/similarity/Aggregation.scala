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

package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair}
import xml.Node
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import de.fuberlin.wiwiss.silk.entity.{Index, Entity}

/**
 * An aggregation combines multiple similarity values into a single value.
 */
case class Aggregation(id: Identifier = Operator.generateId,
                       required: Boolean = false,
                       weight: Int = 1,
                       aggregator: Aggregator,
                       operators: Seq[SimilarityOperator]) extends SimilarityOperator {

  require(weight > 0, "weight > 0")
  //TODO learning currently may produce empty aggreagations when cleaning
  //require(!operators.isEmpty, "!operators.isEmpty")

  def indexing = operators.exists(_.indexing)

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit The similarity threshold.
   *
   * @return The similarity as a value between -1.0 and 1.0.
   *         None, if no similarity could be computed.
   */
  override def apply(entities: DPair[Entity], limit: Double): Option[Double] = {
    val totalWeights = operators.foldLeft(0)(_ + _.weight)

    var weightedValues: List[(Int, Double)] = Nil
    for(op <- operators) {
      val opThreshold = aggregator.computeThreshold(limit, op.weight.toDouble / totalWeights)
      op(entities, opThreshold) match {
        case Some(v) => weightedValues ::= (op.weight, v)
        case None if op.required => return None
        case None =>
      }
    }

    aggregator.evaluate(weightedValues)
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  override def index(entity: Entity, threshold: Double): Index = {
    val totalWeights = operators.map(_.weight).sum

    val indexSets = {
      for (op <- operators if op.indexing) yield {
        val opThreshold = aggregator.computeThreshold(threshold, op.weight.toDouble / totalWeights)
        val index = op.index(entity, opThreshold)

        if (op.required && index.isEmpty) return Index.empty

        index
      }
    }.filterNot(_.isEmpty)

    if (indexSets.isEmpty)
      Index.empty
    else
      indexSets.reduceLeft[Index](aggregator.combineIndexes(_, _))
  }

  override def toXML(implicit prefixes: Prefixes) = aggregator match {
    case Aggregator(func, params) => {
      <Aggregate id={id} required={required.toString} weight={weight.toString} type={func}>
        {operators.map(_.toXML)}
      </Aggregate>
    }
  }
}

object Aggregation {
  def fromXML(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Aggregation = {
    val requiredStr = node \ "@required" text
    val weightStr = node \ "@weight" text

    val aggregator = Aggregator(node \ "@type" text, Operator.readParams(node))

    Aggregation(
      id = Operator.readId(node),
      required = if (requiredStr.isEmpty) false else requiredStr.toBoolean,
      weight = if (weightStr.isEmpty) 1 else weightStr.toInt,
      operators = SimilarityOperator.fromXML(node.child),
      aggregator = aggregator
    )
  }
}
