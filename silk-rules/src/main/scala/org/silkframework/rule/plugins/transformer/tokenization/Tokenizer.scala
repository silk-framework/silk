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

package org.silkframework.rule.plugins.transformer.tokenization

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "tokenize",
  categories = Array("Tokenization", PluginCategories.recommended),
  label = "Tokenize",
  description = "Tokenizes all input values.")
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("Hello World"),
    output = Array("Hello", "World")
  ),
  new TransformExample(
    parameters = Array("regex", ","),
    input1 = Array(".175,.050"),
    output = Array(".175", ".050")
  )
))
case class Tokenizer(
  @Param("The regular expression used to split values.")
  regex: String = "\\s") extends Transformer {

  private[this] val compiledRegex = regex.r

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    values.reduce(_ ++ _).flatMap(compiledRegex.split)
  }
}