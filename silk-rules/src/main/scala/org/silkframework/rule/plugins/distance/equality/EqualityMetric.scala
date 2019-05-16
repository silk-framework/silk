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
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "equality",
  categories = Array("Equality", "Recommended"),
  label = "String Equality",
  description = "Checks for equality of the string representation of the given values. Returns success if string values are equal, failure otherwise. For" +
      " a numeric comparison of values use the 'Numeric Equality' comparator."
)
case class EqualityMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = if (str1 == str2) 0.0 else 1.0

  override def indexValue(str: String, threshold: Double, sourceOrTarget: Boolean): Index = Index.oneDim(Set(str.hashCode))
}
