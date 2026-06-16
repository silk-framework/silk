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

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = CapitalizeTransformer.pluginId,
  categories = Array("Normalize"),
  label = "Capitalize",
  description = "Capitalizes the string i.e. converts the first character to upper case. " +
    "If 'allWords' is set to true, all words are capitalized and not only the first character.",
  relatedPlugins = Array(
    new PluginReference(
      id = LowerCaseTransformer.pluginId,
      description = "Capitalize raises only the first character, or the first of each word, leaving the rest of the string unchanged. Lower case converts every character, making it the right choice when the entire string needs to be normalized rather than just its initial character."
    ),
    new PluginReference(
      id = UpperCaseTransformer.pluginId,
      description = "Capitalize changes only the first character, leaving the rest of the string as-is. Upper case is the right plugin when every character needs to be raised, not just the initial one."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("allWords", "false"),
    input1 = Array("capitalize me"),
    output = Array("Capitalize me")
  ),
  new TransformExample(
    parameters = Array("allWords", "true"),
    input1 = Array("capitalize me"),
    output = Array("Capitalize Me")
  )
))
case class CapitalizeTransformer(allWords: Boolean = false) extends SimpleTransformer {
  override def evaluate(value: String) = {
    if(!allWords)
      value.capitalize
    else
      value.split("\\s+").map(_.capitalize).mkString(" ")
  }
}

object CapitalizeTransformer {
  final val pluginId = "capitalize"
}