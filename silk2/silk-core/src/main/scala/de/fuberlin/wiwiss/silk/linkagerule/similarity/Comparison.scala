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

import de.fuberlin.wiwiss.silk.linkagerule.input.Input
import de.fuberlin.wiwiss.silk.config.Prefixes
import xml.Node
import de.fuberlin.wiwiss.silk.util.{ValidationException, Identifier, DPair}
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import de.fuberlin.wiwiss.silk.entity.{Index, Entity}

/**
 * A comparison computes the similarity of two inputs.
 */
case class Comparison(id: Identifier = Operator.generateId,
                      required: Boolean = false,
                      weight: Int = 1,
                      threshold: Double = 0.0,
                      indexing: Boolean = true,
                      metric: DistanceMeasure,
                      inputs: DPair[Input]) extends SimilarityOperator {

  require(weight > 0, "weight > 0")
  require(threshold >= 0.0, "threshold >= 0.0")

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit The confidence limit.
   *
   * @return The confidence as a value between -1.0 and 1.0.
   */
  override def apply(entities: DPair[Entity], limit: Double): Option[Double] = {
    val values1 = inputs.source(entities)
    val values2 = inputs.target(entities)

    if (values1.isEmpty || values2.isEmpty)
      None
    else {
      val distance = metric(values1, values2, threshold * (1.0 - limit))

      if (distance == 0.0 && threshold == 0.0)
        Some(1.0)
      else if (distance <= 2.0 * threshold)
        Some(1.0 - distance / threshold)
      else if (!required)
        Some(-1.0)
      else
        None
    }
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param threshold The similarity threshold.
   *
   * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  override def index(entity: Entity, limit: Double): Index = {
    val entityPair = DPair.fill(entity)

    val values = inputs.source(entityPair) ++ inputs.target(entityPair)

    val distanceLimit = threshold * (1.0 - limit)

    metric.index(values, distanceLimit)
  }

  override def toXML(implicit prefixes: Prefixes) = metric match {
    case DistanceMeasure(func, params) => {
      <Compare id={id} required={required.toString} weight={weight.toString} metric={func} threshold={threshold.toString}>
        {inputs.source.toXML}{inputs.target.toXML}{params.map {
        case (name, value) => <Param name={name} value={value}/>
      }}
      </Compare>
    }
  }
}

object Comparison {
  def fromXML(node: Node)(implicit prefixes: Prefixes, globalThreshold: Option[Double]): Comparison = {
    val id = Operator.readId(node)
    val inputs = Input.fromXML(node.child)
    if(inputs.size != 2) throw new ValidationException("A comparison must have exactly 2 inputs ", id, "Comparison")

    try {
      val requiredStr = node \ "@required" text
      val threshold = (node \ "@threshold").headOption.map(_.text.toDouble).getOrElse(1.0 - globalThreshold.getOrElse(1.0))
      val weightStr = node \ "@weight" text
      val indexingStr = node \ "@indexing" text
      val metric = DistanceMeasure(node \ "@metric" text, Operator.readParams(node))

      Comparison(
        id = id,
        required = if (requiredStr.isEmpty) false else requiredStr.toBoolean,
        threshold = threshold,
        weight = if (weightStr.isEmpty) 1 else weightStr.toInt,
        indexing = if (indexingStr.isEmpty) true else indexingStr.toBoolean,
        inputs = DPair(inputs(0), inputs(1)),
        metric = metric
      )
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Comparison")
    }
  }
}
