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

package org.silkframework.plugins.transformer.date

import java.text.SimpleDateFormat
import java.util.{Date, GregorianCalendar}

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

import scala.math.BigInt;

/**
 * Convert Unix timestamp to xsd:date.
 *
 * @author Julien Plu
 */
@Plugin(
  id = "timeToDate",
  categories = Array("Date"),
  label = "Timestamp to date",
  description = "convert Unix timestamp to xsd:date"
)
case class TimestampToDateTransformer(format: String = "yyyy-MM-dd") extends SimpleTransformer {
  val sdf = new SimpleDateFormat(format)

  override def evaluate(value: String) = {
    val date = new Date(value.toLong * 1000)
    sdf.format(date)
  }
}
