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

package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(id = "equality", label = "Equality", description = "Return 1 if strings are equal, 0 otherwise.")
case class EqualityMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double) = if (str1 == str2) 0.0 else 1.0

  override def indexValue(str: String, threshold: Double) = Index.oneDim(Set(str.hashCode))
}
