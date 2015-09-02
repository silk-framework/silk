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

package de.fuberlin.wiwiss.silk.plugins.transformer.filter

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.rule.input.Transformer

@Plugin(
  id = "removeEmptyValues",
  categories = Array("Filter"),
  label = "Remove empty values",
  description = "Removes empty values."
)
case class RemoveEmptyValues() extends Transformer {
  override def apply(values: Seq[Set[String]]) = {
    values.head.filter(!_.isEmpty)
  }
}