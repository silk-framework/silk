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
  id = "validateDateRange",
  categories = Array("Validation", "Date"),
  label = "validate date range",
  description = "Validates if dates are within a specified range."
)
case class ValidateDateRange(
  @Param(value = "Earliest allowed date in YYYY-MM-DD", example = "1900-01-01")
  minDate: String,
  @Param(value = "Latest allowed data in YYYY-MM-DD", example = "2000-12-12")
  maxDate: String) extends SimpleTransformer {

  private val min = datatypeFactory.newXMLGregorianCalendar(minDate)

  private val max = datatypeFactory.newXMLGregorianCalendar(maxDate)

  override def evaluate(value: String) = {
    val date = datatypeFactory.newXMLGregorianCalendar(value)

    if(date.compare(min) < 0) {
      throw new ValidationException(s"Date $date is earlier than allowed minimum $min")
    } else if(date.compare(max) > 0) {
      throw new ValidationException(s"Date $date is later than allowed maximum $max")
    } else {
      value
    }
  }
}

object ValidateDateRange {
  private val datatypeFactory = DatatypeFactory.newInstance()
}