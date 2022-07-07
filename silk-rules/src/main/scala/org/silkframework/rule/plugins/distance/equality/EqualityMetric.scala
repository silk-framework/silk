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

package org.silkframework.rule.plugins.distance.equality

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.{DistanceMeasureExample, DistanceMeasureExamples, Plugin}

@Plugin(
  id = "equality",
  categories = Array("Equality", PluginCategories.recommended),
  label = "String equality",
  description = "Checks for equality of the string representation of the given values. Returns success if string values are equal, failure otherwise. For" +
      " a numeric comparison of values use the 'Numeric Equality' comparator."
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Return distance 0, if at least one value matches",
    input1 = Array("max", "helmut"),
    input2 = Array("max"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Return distance 1, if no value matches",
    input1 = Array("max", "helmut"),
    input2 = Array("john"),
    output = 1.0
  )
))
case class EqualityMetric() extends SimpleDistanceMeasure {

  override def apply(values1: Seq[String], values2: Seq[String], limit: Double): Double = {
    if(values2.size == 1) {
      if(values1.contains(values2.head)) {
        0.0
      } else {
        1.0
      }
    } else {
      val values2Set = values2.toSet
      if (values1.exists(values2Set.contains)) {
        0.0
      } else {
        1.0
      }
    }
  }

  override def evaluate(str1: String, str2: String, threshold: Double): Double = if (str1 == str2) 0.0 else 1.0

  override def emptyIndex(limit: Double): Index = Index.oneDim(Set.empty)

  override def indexValue(str: String, threshold: Double, sourceOrTarget: Boolean): Index = Index.oneDim(Set(str.hashCode))
}