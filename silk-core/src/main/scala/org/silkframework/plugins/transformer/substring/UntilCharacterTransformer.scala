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

package org.silkframework.plugins.transformer.substring

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

/**
 * Give a substring until the character given.
 *
 * @author Julien Plu
 */
@Plugin(
  id = "untilCharacter",
  categories = Array("Substring"),
  label = "Until Character",
  description = "Give a substring until the character given"
)
case class UntilCharacterTransformer(untilCharacter: Char) extends SimpleTransformer {
  override def evaluate(value: String) = {
    value.takeWhile(_ != untilCharacter)
  }
}
