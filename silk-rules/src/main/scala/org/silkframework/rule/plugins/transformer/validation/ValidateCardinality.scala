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

package org.silkframework.rule.plugins.transformer.validation

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.{Param, Plugin, TransformExample, TransformExamples}
import org.silkframework.runtime.validation.ValidationException

@Plugin(
  id = "validateCardinality",
  categories = Array("Validation"),
  label = "Validate number of values",
  description = "Validates that the number of values lies in a specified range."
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array("min", "0", "max", "1"),
    input1 = Array("value1"),
    output = Array("value1")
  ),
  new TransformExample(
    parameters = Array("min", "0", "max", "1"),
    input1 = Array("value1", "value2"),
    output = Array(),
    throwsException = "org.silkframework.runtime.validation.ValidationException"
  )
))
case class ValidateCardinality(
  @Param(value = "Minimum allowed number of values")
  min: Int = 0,
  @Param(value = "Maximum allowed number of values")
  max: Int = 1) extends Transformer {

  def apply(values: Seq[Seq[String]]): Seq[String] = {
    val allValues = values.flatten
    val numberOfValues = allValues.size
    if(numberOfValues < min) {
      throw new ValidationException(s"Expected at least $min values. Got $numberOfValues values.")
    } else if(numberOfValues > max) {
      throw new ValidationException(s"Expected at most $max values. Got $numberOfValues values.")
    } else {
      allValues
    }
  }

}
