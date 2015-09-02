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

package de.fuberlin.wiwiss.silk.plugins.distance.numeric

import scala.math._
import javax.xml.datatype.{DatatypeConstants, XMLGregorianCalendar, DatatypeFactory}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.rule.similarity.SimpleDistanceMeasure

@Plugin(
  id = "dateTime",
  categories = Array("Numeric"),
  label = "DateTime",
  description = "Distance between two date time values (xsd:dateTime format) in seconds.")
case class DateTimeMetric() extends SimpleDistanceMeasure {

  private val datatypeFactory = DatatypeFactory.newInstance

  override def evaluate(str1: String, str2: String, threshold: Double) = {
    try {
      val date1 = datatypeFactory.newXMLGregorianCalendar(str1)
      val date2 = datatypeFactory.newXMLGregorianCalendar(str2)

      abs(totalSeconds(date1) - totalSeconds(date2)).toDouble
    }
    catch {
      case ex: IllegalArgumentException => Double.PositiveInfinity
    }
  }

  private def totalSeconds(date: XMLGregorianCalendar) = {
    val seconds = date.getSecond match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case s => s
    }

    val minuteSeconds = date.getMinute match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case m => m * 60
    }

    val hourSeconds = date.getHour match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case h => h * 60 * 60
    }

    val daySeconds = date.getDay match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case d => d * 24 * 60 * 60
    }

    val monthSeconds = date.getMonth match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case m => m * 30 * 24 * 60 * 60
    }

    val yearSeconds = date.getYear match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case y => y * 365 * 24 * 60 * 60
    }

    seconds + minuteSeconds + hourSeconds + daySeconds + monthSeconds + yearSeconds
  }
}