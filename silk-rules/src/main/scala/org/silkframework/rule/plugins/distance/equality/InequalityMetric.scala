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

import org.silkframework.rule.annotations.{DistanceMeasureExample, DistanceMeasureExamples}
import org.silkframework.rule.similarity.{BooleanDistanceMeasure, SingleValueDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "inequality",
  categories = Array("Equality"),
  label = "Inequality",
  description = "Returns success if values are not equal, failure otherwise."
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns distance 0, if the values are different",
    input1 = Array("max"),
    input2 = Array("john"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns distance 1, if the values are equal",
    input1 = Array("max"),
    input2 = Array("max"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "If multiple values are provided, returns 0, if at least one value does not match",
    input1 = Array("max", "helmut"),
    input2 = Array("max"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "If multiple values are provided, returns 1, if all value match",
    input1 = Array("max", "max"),
    input2 = Array("max", "max"),
    output = 1.0
  )
))
case class InequalityMetric() extends SingleValueDistanceMeasure with BooleanDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    if (str1 == str2) 1.0 else 0.0
  }
}
