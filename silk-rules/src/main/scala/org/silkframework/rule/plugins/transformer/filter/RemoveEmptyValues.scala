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

package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}

@Plugin(
  id = "removeEmptyValues",
  categories = Array("Filter"),
  label = "Remove empty values",
  description = "Removes empty values."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("value1", "", "value2"),
    output = Array("value1", "value2")
  ),
  new TransformExample(
    input1 = Array("", ""),
    output = Array()
  )
))
case class RemoveEmptyValues() extends Transformer {
  override def apply(values: Seq[Seq[String]]) = {
    values.head.filter(!_.isEmpty)
  }
}