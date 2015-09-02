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

package de.fuberlin.wiwiss.silk.plugins.distance.equality

import de.fuberlin.wiwiss.silk.rule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "constant",
  categories = Array("Equality"),
  label = "Constant",
  description = "Always returns a constant similarity value."
)
case class ConstantMetric(value: Double = 1.0) extends DistanceMeasure {
  override def apply(values1: Traversable[String], values2: Traversable[String], limit: Double) = {
    value
  }
}
