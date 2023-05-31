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
import org.silkframework.runtime.plugin.annotations.Plugin

/**
 * Give a substring until the character given.
 *
 * @author Julien Plu
 */
@Plugin(
  id = "untilCharacter",
  categories = Array("Substring"),
  label = "Until character",
  description = "Extracts the substring until the character given."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("untilCharacter", "c"),
    input1 = Array("abcde"),
    output = Array("ab")
  ),
  new TransformExample(
    parameters = Array("untilCharacter", "c"),
    input1 = Array("abab"),
    output = Array("abab")
  )
))
case class UntilCharacterTransformer(untilCharacter: Char) extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.takeWhile(_ != untilCharacter)
  }
}
