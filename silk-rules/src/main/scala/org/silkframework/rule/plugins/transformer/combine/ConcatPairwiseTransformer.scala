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

package org.silkframework.rule.plugins.transformer.combine

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import org.silkframework.rule.input.InlineTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

@Plugin(
  id = ConcatPairwiseTransformer.pluginId,
  categories = Array("Combine"),
  label = "Concatenate pairwise",
  description = "Concatenates the values of multiple inputs pairwise.",
  relatedPlugins = Array(
    new PluginReference(
      id = ConcatTransformer.pluginId,
      description = "Concatenate pairwise matches values by position and produces one combined string per position. Concatenate does not align by position — it produces every combination of values across inputs, so two inputs with three values each yield nine strings, not three."
    ),
    new PluginReference(
      id = ZipTransformer.pluginId,
      description = "When inputs have unequal lengths, Concatenate pairwise drops the extra values from the longer input. Zip solves the same alignment problem for exactly two inputs but keeps them by substituting a configurable placeholder for each missing value."
    )
  )
)
@TransformExamples(Array(
  new TransformExample(
    description = "Values of two inputs are concatenated pairwise",
    input1 = Array("a", "b", "c"),
    input2 = Array("1", "2", "3"),
    output = Array("a1", "b2", "c3")
  ),
  new TransformExample(
    description = "More than two inputs are supported as well.",
    input1 = Array("a", "b", "c"),
    input2 = Array("1", "2", "3"),
    input3 = Array("x", "y", "z"),
    output = Array("a1x", "b2y", "c3z")
  ),
  new TransformExample(
    description = "If one of the inputs has more values than the other, its remaining values are ignored",
    input1 = Array("a", "b", "c"),
    input2 = Array("1", "2"),
    output = Array("a1", "b2")
  ),
  new TransformExample(
    description = "Empty input leads to empty output",
    output = Array()
  ),
  new TransformExample(
    description = "A single input is just forwarded",
    input1 = Array("a"),
    output = Array("a")
  ),
))
case class ConcatPairwiseTransformer(
  @Param("Separator to be inserted between two concatenated strings. The text can contain escaped characters \\n, \\t and" +
     " \\\\ that are replaced by a newline, tab or backslash respectively.")
  glue: String = "") extends InlineTransformer {

  // glue with escaped char sequences (\\, \n, \t) converted to actual character.
  private lazy val parsedGlue: String = ConcatTransformer.parseGlue(glue)

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    if(values.isEmpty) {
      Seq.empty
    } else if(values.size == 1) {
      values.head
    } else {
      values.reduce((v1, v2) => v1.zip(v2).map(v => v._1 + parsedGlue + v._2))
    }
  }
}

object ConcatPairwiseTransformer {
  final val pluginId = "concatPairwise"
}
