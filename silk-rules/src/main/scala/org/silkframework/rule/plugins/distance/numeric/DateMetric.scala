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

package org.silkframework.rule.plugins.distance.numeric

import java.time.{DateTimeException, LocalDate}
import java.time.temporal.ChronoUnit

import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.Plugin

import scala.math._

@Plugin(
  id = "date",
  categories = Array("Numeric"),
  label = "Date",
  description = "The distance in days between two dates ('YYYY-MM-DD' format).")
case class DateMetric() extends SimpleDistanceMeasure {

  import DateMetric._

  private val minDays = 0

  private val maxDays = 3000 * 365

  private final val millisPerDay = 1000 * 60 * 60 * 24

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    try {
      val date1 = parseDate(str1)
      val date2 = parseDate(str2)
      abs(ChronoUnit.DAYS.between(date1, date2)).toDouble
    } catch {
      case _: IllegalArgumentException => Double.PositiveInfinity
      case _: DateTimeException => Double.PositiveInfinity
    }
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    try {
      val date = dataTypeFactory.newXMLGregorianCalendar(str)
      val days = date.toGregorianCalendar.getTimeInMillis / millisPerDay
      Index.continuous(days, minDays, maxDays, limit)
    } catch {
      case ex: IllegalArgumentException => Index.empty
    }
  }

  @inline
  private def parseDate(str: String): LocalDate = {
    val date = dataTypeFactory.newXMLGregorianCalendar(str)
    LocalDate.of(date.getYear, date.getMonth, date.getDay)
  }
}

object DateMetric {
  private val dataTypeFactory = DatatypeFactory.newInstance
}
