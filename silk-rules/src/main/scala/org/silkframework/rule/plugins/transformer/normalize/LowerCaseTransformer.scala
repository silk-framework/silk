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
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "lowerCase",
  categories = Array("Normalize", PluginCategories.recommended),
  label = "Lower case",
  description = "Converts a string to lower case."
)
@TransformExamples(Array(
  new TransformExample(
    description = "Transforms all values to lower case.",
    input1 = Array("JoHN", "LeNA"),
    output = Array("john", "lena")
  )
))
case class LowerCaseTransformer() extends SimpleTransformer {

  override def evaluate(value: String): String = {
    value.toLowerCase
  }
}
