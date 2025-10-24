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

package org.silkframework.rule.plugins.transformer.replace

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "replace",
  categories = Array("Replace"),
  label = "Replace",
  description = "Replace all occurrences of a string with another string."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array(
      "search", " ",
      "replace", ""
    ),
    input1 = Array("a b c"),
    output = Array("abc")
  ),
  new TransformExample(
    parameters = Array(
      "search", "abc",
      "replace", ""
    ),
    input1 = Array("abcdef"),
    output = Array("def")
  )
))
case class ReplaceTransformer(@Param(value = "The string to search for")
                              search: String,
                              @Param(value = "The replacement of each match")
                              replace: String) extends SimpleTransformer {

  override def evaluate(value: String): String = {
    value.replace(search, replace)
  }
}
