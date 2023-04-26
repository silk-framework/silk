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

import org.silkframework.rule.annotations.{TransformExample, TransformExamples}
import java.text.SimpleDateFormat
import java.util.Date
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

import java.time.Instant

/**
 * Convert Unix timestamp to xsd:date.
 *
 * @author Julien Plu
 */
@Plugin(
  id = "timeToDate",
  categories = Array("Date"),
  label = "Timestamp to date",
  description = "Convert a timestamp to xsd:date format. Expects an integer that denotes the passed time since the Unix Epoch (1970-01-01)"
)
@TransformExamples(Array(
  new TransformExample(
    parameters = Array(),
    input1 = Array("1499117572000"),
    output = Array("2017-07-03T21:32:52Z")
  ),
  new TransformExample(
    parameters = Array("format", "yyyy-MM-dd"),
    input1 = Array("1499040000000"),
    output = Array("2017-07-03")
  ),
  new TransformExample(
    parameters = Array("format", "yyyy-MM-dd", "unit", "seconds"),
    input1 = Array("1499040000"),
    output = Array("2017-07-03")
  )
))
case class TimestampToDateTransformer(
  @Param("Custom output format (e.g., 'yyyy-MM-dd'). If left empty, a full xsd:dateTime (UTC) is returned.")
  format: String = "",
  unit: DateUnit = DateUnit.milliseconds
) extends SimpleTransformer {

  private val dateFormat = {
    if(format.trim.isEmpty) {
      None
    } else {
      Some(new SimpleDateFormat(format))
    }
  }

  override def evaluate(value: String): String = {
    val instant = Instant.EPOCH.plus(value.toLong, unit.toChronoUnit)
    dateFormat match {
      case Some(df) =>
        df.format(new Date(instant.toEpochMilli))
      case None =>
        instant.toString
    }
  }
}
