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

package de.fuberlin.wiwiss.silk.plugins.transformer.replace

import de.fuberlin.wiwiss.silk.rule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

@Plugin(
  id = "replace",
  categories = Array("Replace"),
  label = "Replace",
  description = "Replace all occurrences of a string \"search\" with \"replace\" in a string."
)
case class ReplaceTransformer(search: String, replace: String) extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.replace(search, replace)
  }
}
