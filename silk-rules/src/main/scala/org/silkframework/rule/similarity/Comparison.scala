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
import org.silkframework.rule.Operator
import org.silkframework.rule.input.Input
import org.silkframework.runtime.plugin.PluginBackwardCompatibility
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{DPair, Identifier}

import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.Node

/**
 * A comparison computes the similarity of two inputs.
 */
case class Comparison(id: Identifier = Operator.generateId,
                      weight: Int = 1,
                      threshold: Double = 0.0,
                      indexing: Boolean = true,
                      metric: DistanceMeasure,
                      inputs: DPair[Input]) extends SimilarityOperator {

  require(weight > 0, "weight > 0")
  require(threshold >= 0.0, "threshold >= 0.0")

  private val sourceInput = inputs.source
  private val targetInput = inputs.target

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit The confidence limit.
   * @return The confidence as a value between -1.0 and 1.0.
   */
  override def apply(entities: DPair[Entity], limit: Double): Option[Double] = {
    val values1 =
      try {
        sourceInput.apply(entities.source)
      } catch {
        case NonFatal(_) =>
          Seq.empty
      }
    val values2 =
      try {
        targetInput.apply(entities.target)
      } catch {
        case NonFatal(_) =>
          Seq.empty
      }

    if (values1.isEmpty || values2.isEmpty) {
      None
    } else {
      val distance = metric(values1, values2, threshold * (1.0 - limit))

      if (distance == 0.0 && threshold == 0.0)
        Some(1.0)
      else if (distance <= 2.0 * threshold)
        Some(1.0 - distance / threshold)
      else
        Some(-1.0)
    }
  }

  /**
    * Indexes an entity.
    *
    * @param entity         The entity to be indexed
    * @param limit          The similarity threshold.
    * @param sourceOrTarget If true the value comes from the source, else from the target.
    * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
    */
  override def index(entity: Entity, sourceOrTarget: Boolean, limit: Double): Index = {
    val values = Try(inputs.select(sourceOrTarget)(entity)).getOrElse(Seq.empty)

    val distanceLimit = threshold * (1.0 - limit)

    metric.index(values, distanceLimit, sourceOrTarget)
  }

  override def children = inputs.toSeq

  override def withChildren(newChildren: Seq[Operator]) = {
    copy(inputs = DPair.fromSeq(newChildren.collect{ case input: Input => input }))
  }
}

object Comparison {

  /**
   * XML serialization format.
   */
  implicit object ComparisonFormat extends XmlFormat[Comparison] {

    import XmlSerialization._

    def read(node: Node)(implicit readContext: ReadContext): Comparison = {
      val id = Operator.readId(node)
      val inputs = node.child.filter(n => n.label == "Input" || n.label == "TransformInput").map(fromXml[Input]).toIndexedSeq
      if(inputs.size != 2) throw new ValidationException("A comparison must have exactly two inputs ", id, "Comparison")
      implicit val prefixes = readContext.prefixes
      implicit val resourceManager = readContext.resources

      try {
        val threshold = (node \ "@threshold").headOption.map(_.text.toDouble).getOrElse(0.0)
        val weightStr = (node \ "@weight").text
        val indexingStr = (node \ "@indexing").text
        val metricPluginId = (node \ "@metric").text
        val metric = DistanceMeasure(PluginBackwardCompatibility.distanceMeasureIdMapping.getOrElse(metricPluginId, metricPluginId), Operator.readParams(node))

        Comparison(
          id = id,
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

    def write(value: Comparison)(implicit writeContext: WriteContext[Node]): Node = {
      value.metric match {
        case DistanceMeasure(plugin, params) =>
          <Compare id={value.id} weight={value.weight.toString} metric={plugin.id} threshold={value.threshold.toString} indexing={value.indexing.toString}>
            {toXml(value.inputs.source)}{toXml(value.inputs.target)}{XmlSerialization.serializeParameter(params)}
          </Compare>
      }
    }
  }
}
