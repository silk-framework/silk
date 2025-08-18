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

import scala.util.matching.Regex

@Plugin(
  id = "regexReplace",
  categories = Array("Replace"),
  label = "Regex replace",
  description = "Replace all occurrences of a regular expression in a string.",
  documentationFile = "RegexReplaceTransformer.md"
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("regex", "[^0-9]*"),
    input1 = Array("a0b1c2"),
    output = Array("012")
  ),
  new TransformExample(
    parameters = Array("regex", "[a-z]*"),
    input1 = Array("abcdef1"),
    output = Array("1")
  ),
))
case class RegexReplaceTransformer(@Param(value = "The regular expression to search for", example = "\\s*")
                                   regex: String,
                                   @Param(value = "The string that will replace each match")
                                   replace: String = "") extends SimpleTransformer{
  private val compiledRegex = new Regex(regex)

  def evaluate(value: String): String = {
    compiledRegex.replaceAllIn(value, replace)
  }
}
