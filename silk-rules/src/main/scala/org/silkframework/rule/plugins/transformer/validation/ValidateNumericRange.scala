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

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.rule.plugins.transformer.numeric.CompareNumbersTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.validation.ValidationException

@Plugin(
  id = ValidateNumericRange.pluginId,
  categories = Array("Validation", "Date"),
  label = "Validate numeric range",
  description = "Validates if a number is within a specified range.",
  relatedPlugins = Array(
    new PluginReference(
      id = CompareNumbersTransformer.pluginId,
      description = "Validate numeric range either passes a number through or throws, producing no output on violation. Compare numbers always produces a 1 or 0 regardless of which side is larger, so the downstream pipeline continues in either case."
    )
  )
)
case class ValidateNumericRange(
  @Param(value = "Minimum allowed number", example = "0.0")
  min: Double,
  @Param(value = "Maximum allowed number", example = "100.0")
  max: Double) extends SimpleTransformer {

  override def evaluate(value: String) = {
    val num =
      try {
        value.toDouble
      } catch {
        case _: NumberFormatException =>
          throw new ValidationException(s"'$value' is not a valid number.")
      }

    if(num < min) {
      throw new ValidationException(s"Number $num is smaller than allowed minimum $min")
    } else if(num > max) {
      throw new ValidationException(s"Number $num is larger than allowed maximum $max")
    } else {
      value
    }
  }
}

object ValidateNumericRange {
  final val pluginId = "validateNumericRange"
}
