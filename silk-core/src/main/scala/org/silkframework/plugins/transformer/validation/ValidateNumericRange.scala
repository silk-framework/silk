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

package org.silkframework.plugins.transformer.validation

import javax.xml.datatype.DatatypeFactory

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.{Param, Plugin}
import ValidateDateRange._
import org.silkframework.runtime.validation.ValidationException

@Plugin(
  id = "validateNumericRange",
  categories = Array("Validation", "Date"),
  label = "validate numeric range",
  description = "Validates if a number is within a specified range."
)
case class ValidateNumericRange(
  @Param(value = "Minimum allowed number", example = "0.0")
  min: Double,
  @Param(value = "Maximum allowed number", example = "100.0")
  max: Double) extends SimpleTransformer {

  override def evaluate(value: String) = {
    val num = value.toDouble
    if(num < min) {
      throw new ValidationException(s"Number $num is larger than allowed minimum $min")
    } else if(num > max) {
      throw new ValidationException(s"Number $num is smaller than allowed maximum $max")
    } else {
      value
    }
  }
}
