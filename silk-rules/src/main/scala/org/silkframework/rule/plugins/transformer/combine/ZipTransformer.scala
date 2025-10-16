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
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

@Plugin(
  id = "zip",
  categories = Array("Combine"),
  label = "Zip",
  description = "Concatenates the values of two inputs in pairs."
)
@TransformExamples(Array(
  new TransformExample(
    description = "Zipping two inputs of equal length.",
    input1 = Array("a", "b"),
    input2 = Array("1", "2"),
    output = Array("a1", "b2")
  ),
  new TransformExample(
    description = "Zipping two inputs of different lengths, using placeholders.",
    parameters = Array("firstPlaceholder", "_", "secondPlaceholder", "-"),
    input1 = Array("a", "b", "c"),
    input2 = Array("1", "2"),
    output = Array("a1", "b2", "c-")
  ),
))
case class ZipTransformer(@Param(value = "Placeholder to be used if the first input provides fewer values than the second one.")
                          firstPlaceholder: String = "",
                          @Param(value = "Placeholder to be used if the second input provides fewer values than the first one.")
                          secondPlaceholder: String = "") extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    require(values.size == 2, "Zip transformer requires exactly two inputs.")
    val firstValues = values.head
    val secondValues = values(1)
    firstValues.zipAll(secondValues, firstPlaceholder, secondPlaceholder) map { case (v1, v2) => v1 + v2 }
  }
}