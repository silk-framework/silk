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

import org.silkframework.rule.input.{SimpleTransformer}
import org.silkframework.runtime.plugin.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "capitalize",
  categories = Array("Normalize"),
  label = "Capitalize",
  description = "Capitalizes the string i.e. converts the first character to upper case. " +
    "If 'allWords' is set to true, all words are capitalized and not only the first character."
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