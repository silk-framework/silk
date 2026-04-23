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

package org.silkframework.rule.plugins.transformer.normalize

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = RemoveBlanksTransformer.pluginId,
  categories = Array("Normalize"),
  label = "Remove blanks",
  description = "Remove whitespace from a string.",
  relatedPlugins = Array(
    new PluginReference(
      id = TrimTransformer.pluginId,
      description = "Remove blanks removes only plain space characters and does so throughout the entire string regardless of position. Trim is the choice when only the surrounding whitespace needs to go and the internal structure must be preserved; unlike Remove blanks, it also handles tabs and newlines at the edges."
    )
  )
)
case class RemoveBlanksTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.replace(" ", "")
  }
}

object RemoveBlanksTransformer {
  final val pluginId = "removeBlanks"
}
