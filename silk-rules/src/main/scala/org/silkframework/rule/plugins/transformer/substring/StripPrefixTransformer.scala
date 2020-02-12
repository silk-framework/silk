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

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "stripPrefix",
  categories = Array("Substring"),
  label = "Strip prefix",
  description = "Strips a prefix of a string."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("prefix", "prefix"),
    input1 = Array("prefixValue"),
    output = Array("Value")
  ),
  new TransformExample(
    parameters = Array("prefix", "prefix"),
    input1 = Array("ValueWithoutPrefix"),
    output = Array("ValueWithoutPrefix")
  )
))
case class StripPrefixTransformer(prefix: String) extends SimpleTransformer {
  override def evaluate(value: String): String = {
    value.stripPrefix(prefix)
  }
}