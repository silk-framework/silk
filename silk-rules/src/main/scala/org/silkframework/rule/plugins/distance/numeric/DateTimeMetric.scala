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

import org.silkframework.rule.annotations.{DistanceMeasureExample, DistanceMeasureExamples}
import org.silkframework.rule.similarity.SingleValueDistanceMeasure
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

import javax.xml.datatype.{DatatypeConstants, DatatypeFactory}
import scala.math._
import DateTimeMetric._

@Plugin(
  id = DateTimeMetric.pluginId,
  categories = Array("Numeric"),
  label = "DateTime",
  description = "Distance between two date time values (xsd:dateTime format) in seconds.",
  relatedPlugins = Array(
    new PluginReference(
      id = DateMetric.pluginId,
      description = "Where the date time metric plugin demands full datetime values and measures in seconds, the date metric plugin works at day granularity and accepts year-only or year-month dates."
    )
  )
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0 for equal date times.",
    input1 = Array("2010-09-24T05:00:00"),
    input2 = Array("2010-09-24T05:00:00"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns the distance in seconds.",
    input1 = Array("2001-10-26T21:32:10"),
    input2 = Array("2001-10-26T21:32:40"),
    output = 30.0
  ),
  new DistanceMeasureExample(
    description = "Date times crossing a month boundary are one day (86400 seconds) apart.",
    input1 = Array("2020-01-31T00:00:00"),
    input2 = Array("2020-02-01T00:00:00"),
    output = 86400.0
  ),
  new DistanceMeasureExample(
    description = "Explicit timezone offsets are taken into account.",
    input1 = Array("2020-01-01T00:00:00Z"),
    input2 = Array("2020-01-01T02:00:00+02:00"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Invalid date times do not match.",
    input1 = Array("2020-01-01T00:00:00"),
    input2 = Array("not a date"),
    output = Double.PositiveInfinity
  )
))
case class DateTimeMetric() extends SingleValueDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    try {
      abs(epochSeconds(str1) - epochSeconds(str2)).toDouble
    }
    catch {
      case ex: IllegalArgumentException => Double.PositiveInfinity
    }
  }

  private def epochSeconds(str: String): Long = {
    val date = dataTypeFactory.newXMLGregorianCalendar(str)
    // Pin timezone-less values to UTC so distances do not depend on the server timezone.
    if(date.getTimezone == DatatypeConstants.FIELD_UNDEFINED) {
      date.setTimezone(0)
    }
    date.toGregorianCalendar.getTimeInMillis / 1000
  }
}

object DateTimeMetric {
  final val pluginId = "dateTime"
  private val dataTypeFactory = DatatypeFactory.newInstance
}