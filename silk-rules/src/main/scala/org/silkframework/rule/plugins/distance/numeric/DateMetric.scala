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

import javax.xml.datatype.{DatatypeConstants, DatatypeFactory, XMLGregorianCalendar}
import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.{Param, Plugin}

import scala.math._

@Plugin(
  id = "date",
  categories = Array("Numeric"),
  label = "Date",
  description = "The distance in days between two dates ('YYYY-MM-DD' format). If the month or day is mi")
case class DateMetric(
  @Param("If true, no distance value will be generated if months or days are missing (e.g., 2019-11). If false, missing month or day fields will default to 1.")
  requireMonthAndDay: Boolean = false
  ) extends SimpleDistanceMeasure {

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

  // Performance could be improved by using parseDate() instead of toGregorianCalendar.getTimeInMillis, which is slow.
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
    val month = if(date.getMonth == DatatypeConstants.FIELD_UNDEFINED && !requireMonthAndDay) 1 else date.getMonth
    val day = if(date.getDay == DatatypeConstants.FIELD_UNDEFINED && !requireMonthAndDay) 1 else date.getDay
    LocalDate.of(date.getYear, month, day)
  }
}

object DateMetric {
  private val dataTypeFactory = DatatypeFactory.newInstance
}
