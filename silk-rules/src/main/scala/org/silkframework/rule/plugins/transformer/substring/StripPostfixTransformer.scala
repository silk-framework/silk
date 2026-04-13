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

package org.silkframework.rule.plugins.transformer.substring

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = StripPostfixTransformer.pluginId,
  categories = Array("Substring"),
  label = "Strip postfix",
  description = "Strips a postfix of a string.",
  relatedPlugins = Array(
    new PluginReference(
      id = StripPrefixTransformer.pluginId,
      description = "Strip postfix removes from the end; Strip prefix removes from the start. Both leave the value unchanged when the configured string is not found at the expected position."
    ),
    new PluginReference(
      id = SubstringTransformer.pluginId,
      description = "Strip postfix checks for a specific string at the end before removing it. Substring does not check content: a negative end index removes a fixed character count from the end unconditionally."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("postfix", "Postfix"),
    input1 = Array("valuePostfix"),
    output = Array("value")
  ),
  new TransformExample(
    parameters = Array("postfix", "Postfix"),
    input1 = Array("Value"),
    output = Array("Value")
  )
))
case class StripPostfixTransformer(postfix: String) extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.stripSuffix(postfix)
  }
}

object StripPostfixTransformer {
  final val pluginId = "stripPostfix"
}