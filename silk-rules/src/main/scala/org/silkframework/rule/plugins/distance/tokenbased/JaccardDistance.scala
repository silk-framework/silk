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

package org.silkframework.rule.plugins.distance.tokenbased

import org.silkframework.entity.Index
import org.silkframework.rule.annotations.{DistanceMeasureExample, DistanceMeasureExamples}
import org.silkframework.rule.similarity.{DistanceMeasure, NormalizedDistanceMeasure, TokenBasedDistanceMeasure}
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "jaccard",
  categories = Array("Tokenbased", PluginCategories.recommended),
  label = "Jaccard",
  description = "Jaccard similarity coefficient. Divides the matching tokens by the number of distinct tokens from both inputs."
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0 for equal sets of values.",
    input1 = Array("A", "B", "C"),
    input2 = Array("B", "C", "A"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1 if there is no overlap between both sets of tokens.",
    input1 = Array("A", "B", "C"),
    input2 = Array("D", "E", "F"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "Returns 0.5 if half of all unique tokens overlap.",
    input1 = Array("A", "B", "C"),
    input2 = Array("A", "B", "D"),
    output = 0.5
  ),
  new DistanceMeasureExample(
    description = "Returns 2/3 if one third of all unique tokens overlap.",
    input1 = Array("John", "Jane"),
    input2 = Array("John", "Max"),
    output = 2.0 / 3.0
  )
))
case class JaccardDistance() extends DistanceMeasure with TokenBasedDistanceMeasure with NormalizedDistanceMeasure {

  override def apply(values1: Seq[String], values2: Seq[String], limit: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size
    val unionSize = (set1 union set2).size

    1.0 - intersectionSize.toDouble / unionSize
  }

  override def index(values: Seq[String], limit: Double, sourceOrTarget: Boolean): Index = {
    val valuesSet = values.toSet
    //The number of values we need to index
    val indexSize = math.round(valuesSet.size * limit + 0.5).toInt

    Index.oneDim(valuesSet.take(indexSize).map(_.hashCode))
  }
}