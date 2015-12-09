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

package org.silkframework.plugins.distance.tokenbased

import org.silkframework.runtime.plugin.Plugin
import org.silkframework.rule.similarity.DistanceMeasure
import org.silkframework.entity.Index

@Plugin(
  id = "dice",
  categories = Array("Tokenbased"),
  label = "Dice coefficient",
  description = "Dice similarity coefficient."
)
case class DiceCoefficient() extends DistanceMeasure {
  override def apply(values1: Seq[String], values2: Seq[String], threshold: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size * 2
    val totalSize = set1.size + set2.size

    1.0 - intersectionSize.toDouble / totalSize
  }

  override def index(values: Seq[String], limit: Double): Index = {
    val valueSet = values.toSet
    //The number of values we need to index
    val indexSize = math.round((2.0 * valueSet.size * limit / (1 + limit)) + 0.5).toInt

    Index.oneDim(valueSet.take(indexSize).map(value => value.hashCode))
  }
}