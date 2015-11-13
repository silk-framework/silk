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

package de.fuberlin.wiwiss.silk.plugins.temporal.utils

import java.text.{ParseException, SimpleDateFormat}
import java.util.Date
import java.util.logging.Logger
import javax.xml.datatype.DatatypeFactory

import scala.util.Try

/**
 * A time parser.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

object Parser {

  private val logger = Logger.getLogger(this.getClass.getName)

  private val datatypeFactory = DatatypeFactory.newInstance()

  /**
   * This function parses a time String.
   *
   * @param timeString : String
   * @return (Date, Date)
   */
  def parseTime(timeString: String): (Date, Date) = {

    try {

      var period = parsePeriod(timeString)
      if (period == null) {
        val instant = parseInstant(timeString)
        period = (instant, instant)
      }

      period

    } catch {
      case e: Exception => null
    }
  }

  /**
   * This function parses a period String.
   *
   * @param periodString : String
   * @return (Date, Date)
   */
  def parsePeriod(periodString: String): (Date, Date) = {
    try {
      //Remove brackets and split to instants.
      val instants = periodString.substring(1, periodString.length() - 1).split(Constants.PERIOD_DELIM)
      instants.length match {
        case 2 => (parseInstant(instants.head.trim), parseInstant(instants.last.trim))
        case _ => null
      }
    } catch {
      case e: Exception => null
    }
  }

  /**
   * This function parses an instant String.
   *
   * @param instantString : String
   * @return (Date, Date)
   */
  def parseInstant(instantString: String): Date = {
    Try(datatypeFactory.newXMLGregorianCalendar(instantString).toGregorianCalendar.getTime).getOrElse(null)
  }
}