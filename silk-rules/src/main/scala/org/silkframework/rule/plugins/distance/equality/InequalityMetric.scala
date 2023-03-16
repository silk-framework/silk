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

import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.annotations.{DistanceMeasurePlugin, DistanceMeasureRange, Plugin}

@Plugin(
  id = "inequality",
  categories = Array("Equality"),
  label = "Inequality",
  description = "Returns success if values are not equal, failure otherwise."
)
@DistanceMeasurePlugin(
  range = DistanceMeasureRange.BOOLEAN
)
case class InequalityMetric() extends SimpleDistanceMeasure {
  override def evaluate(str1: String, str2: String, threshold: Double) = if (str1 == str2) 1.0 else 0.0
}
