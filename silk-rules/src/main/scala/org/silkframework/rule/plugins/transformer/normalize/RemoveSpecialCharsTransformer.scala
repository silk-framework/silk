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
import org.silkframework.rule.plugins.transformer.linguistic.NormalizeCharsTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

import scala.util.matching.Regex

@Plugin(
  id = RemoveSpecialCharsTransformer.pluginId,
  categories = Array("Normalize"),
  label = "Remove special chars",
  description = "Remove special characters (including punctuation) from a string.",
  relatedPlugins = Array(
    new PluginReference(
      id = NormalizeCharsTransformer.pluginId,
      description = "Remove special chars keeps diacritical characters intact: because they qualify as Unicode letters, they are neither removed nor modified. Normalize chars is the right choice when diacritics need to be converted to their ASCII base forms rather than preserved as-is."
    ),
    new PluginReference(
      id = AlphaReduceTransformer.pluginId,
      description = "The two plugins differ on digits and spaces: Remove special chars keeps digits and removes spaces; Strip non-alphabetic characters removes digits and keeps spaces. Strip non-alphabetic characters is the right tool when word spacing matters and digits do not belong in the output."
    )
  )
)
case class RemoveSpecialCharsTransformer() extends SimpleTransformer {
  private val compiledRegex = new Regex("[^\\d\\pL\\w]+")

  def evaluate(value: String): String = {
    compiledRegex.replaceAllIn(value, "")
  }
}

object RemoveSpecialCharsTransformer {
  final val pluginId = "removeSpecialChars"
}
