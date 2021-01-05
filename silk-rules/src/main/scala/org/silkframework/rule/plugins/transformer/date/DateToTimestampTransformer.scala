/*
 * Copyright 2013 dmdp9553.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.plugins.transformer.date

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, TransformExample, TransformExamples}
import org.silkframework.runtime.validation.ValidationException

import java.time.{Instant, LocalDate, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeParseException

@Plugin(
  id = "datetoTimestamp",
  categories = Array("Date"),
  label = "Date to timestamp",
  description = "Convert an xsd:dateTime to a timestamp. Returns the passed time since the Unix Epoch (1970-01-01)."
)
@TransformExamples(Array(
  new TransformExample(
    input1 = Array("2017-07-03T21:32:52Z"),
    output = Array("1499117572000")
  ),
  new TransformExample(
    input1 = Array("2017-07-03T21:32:52+01:00"),
    output = Array("1499113972000")
  ),
  new TransformExample(
    parameters = Array("unit", "seconds"),
    input1 = Array("2017-07-03T21:32:52+01:00"),
    output = Array("1499113972")
  ),
  new TransformExample(
    input1 = Array("2017-07-03"),
    output = Array("1499040000000")
  )
))
case class DateToTimestampTransformer(unit: DateUnit = DateUnit.milliseconds) extends SimpleTransformer {

  override def evaluate(value: String): String = {
    val dateTime = {
      try {
        ZonedDateTime.parse(value)
      } catch {
        case ex: DateTimeParseException =>
          try {
            // For backward compatibility, we also support pure dates without timezones and assume UTC in that case.
            LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC)
          } catch {
            case _: DateTimeParseException =>
              throw new ValidationException("Invalid date format! Expects an xsd:dateTime that includes a timezone (e.g., 2017-07-03T21:32:52+01:00)." +
                "For backward compatibility, xsd:date without a timezone is supported as well.", ex)
          }
      }
    }

    Instant.EPOCH.until(dateTime, unit.toChronoUnit).toString
  }
}
