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

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "merge",
  categories = Array("Combine"),
  label = "Merge",
  description = "Merges the values of all inputs."
)
@TransformExamples(Array(
  new TransformExample(
    output = Array()
  ),
  new TransformExample(
    input1 = Array("a", "b"),
    input2 = Array("c"),
    output = Array("a", "b", "c")
  )
))
case class MergeTransformer() extends Transformer {
  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    if(values.nonEmpty) {
      values.reduce(_ union _)
    } else {
      Seq.empty
    }

  }
}