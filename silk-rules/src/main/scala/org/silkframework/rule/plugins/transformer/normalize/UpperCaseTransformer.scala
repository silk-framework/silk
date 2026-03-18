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
  id = UpperCaseTransformer.pluginId,
  categories = Array("Normalize"),
  label = "Upper case",
  description = "Converts a string to upper case.",
  relatedPlugins = Array(
    new PluginReference(
      id = LowerCaseTransformer.pluginId,
      description = "Upper case and Lower case are exact complements — one raises all characters, the other lowers them. Lower case is the choice when uniform lowercase is the target."
    ),
    new PluginReference(
      id = CapitalizeTransformer.pluginId,
      description = "Upper case raises every character to upper case. Capitalize raises only the first character of the string, leaving the rest in whatever case it arrives in."
    )
  )
)
case class UpperCaseTransformer() extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.toUpperCase
  }
}

object UpperCaseTransformer {
  final val pluginId = "upperCase"
}
