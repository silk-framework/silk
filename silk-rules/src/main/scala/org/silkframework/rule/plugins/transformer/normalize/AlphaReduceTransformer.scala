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
  id = AlphaReduceTransformer.pluginId,
  categories = Array("Normalize"),
  label = "Strip non-alphabetic characters",
  description = "Strips all non-alphabetic characters from a string. Spaces are retained.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveSpecialCharsTransformer.pluginId,
      description = "Strip non-alphabetic characters removes digits along with punctuation. Remove special chars keeps digits but does not preserve spaces. When numeric content in the string needs to survive, Remove special chars is the applicable plugin."
    ),
    new PluginReference(
      id = NormalizeCharsTransformer.pluginId,
      description = "Strip non-alphabetic characters removes digits and punctuation but does not normalize the letters it keeps; a diacritical letter in the input is a diacritical letter in the output. Normalize chars addresses that: it converts diacritical characters to ASCII equivalents, though it does not strip any content."
    )
  )
)
case class AlphaReduceTransformer() extends SimpleTransformer {
  private val compiledRegex = new Regex("[^\\s\\pL]+")

  override def evaluate(value: String): String = {
    compiledRegex.replaceAllIn(value, "")
  }
}

object AlphaReduceTransformer {
  final val pluginId = "alphaReduce"
}
