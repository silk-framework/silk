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
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.validation.ValidationException

import scala.util.matching.Regex

@Plugin(
  id = "validateRegex",
  categories = Array("Validation"),
  label = "Validate regex",
  description = "Validates if all values match a regular expression."
)
case class ValidateRegex(
  @Param(value = "regular expression")
  regex: String = "\\w*") extends SimpleTransformer {

  private val compiledRegex = new Regex(regex)

  override def evaluate(value: String) = {
    if(!compiledRegex.pattern.matcher(value).matches()) {
      throw new ValidationException(s"Value '$value' did not match regular expression '$regex'.")
    } else {
      value
    }
  }
}
