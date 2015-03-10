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

import javax.xml.datatype.DatatypeFactory

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

import scala.math._

@Plugin(
  id = "date",
  categories = Array("Numeric"),
  label = "Date",
  description = "The distance in days between two dates ('YYYY-MM-DD' format).")
case class DateMetric() extends SimpleDistanceMeasure {

  private val datatypeFactory = DatatypeFactory.newInstance

  private val minDays = 0

  private val maxDays = 3000 * 365

  private final val millisPerDay = 1000 * 60 * 60 * 24

  override def evaluate(str1: String, str2: String, threshold: Double) = {
    try {
      val date1 = datatypeFactory.newXMLGregorianCalendar(str1)
      val date2 = datatypeFactory.newXMLGregorianCalendar(str2)

      val days1 = date1.toGregorianCalendar.getTimeInMillis / millisPerDay
      val days2 = date2.toGregorianCalendar.getTimeInMillis / millisPerDay

      abs(days1 - days2).toDouble
    } catch {
      case ex: IllegalArgumentException => Double.PositiveInfinity
    }
  }

  override def indexValue(str: String, limit: Double): Index = {
    try {
      val date = datatypeFactory.newXMLGregorianCalendar(str)
      val days = date.toGregorianCalendar.getTimeInMillis / millisPerDay
      Index.continuous(days, minDays, maxDays, limit)
    } catch {
      case ex: IllegalArgumentException => Index.empty
    }
  }
}
