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

package de.fuberlin.wiwiss.silk.util.temporal

import java.util.Date
import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.util.temporal.Constants._
import de.fuberlin.wiwiss.silk.util.temporal.Parser.parseTime

/**
 * Useful utils for the temporal extensions of Silk.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

object TemporalExtensionsUtils {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
   * This function indexes times. Assumes that all the times are between "the epoch" (January 1, 1970, 00:00:00 GMT) and "NOW".
   *
   * @param timeString : String
   * @param distance: Double
   * @return Index
   */
  def indexTimesByInterval(timeString: String, distance: Double): Index = {
    try {
      val period = parseTime(timeString)

      //Ensure that period is well-defined.
      if (period == null)
        return Index.empty

      val (start, end) = period

      val blockCount = TIME_RANGE
      val blocks = for (i <- start.getDate() to end.getDate()) yield i

      Index.oneDim(blocks.toSet, blockCount)

    } catch {
      case e: Exception =>
        Index.empty
    }
  }

  /**
   * This function indexes times. Assumes that all the times are between "the epoch" (January 1, 1970, 00:00:00 GMT) and "NOW".
   *
   * @param timeString : String
   * @param distance: Double
   * @return Index
   */
  def indexTimesByPeriodCentre(timeString: String, distance: Double): Index = {
    try {
      val period = parseTime(timeString)

      //Ensure that period is well-defined.
      if (period == null)
        return Index.empty

      val (start, end) = period

      Index.continuous((start.getDate() + end.getDate()) / 2.0, MIN_TIME, MAX_TIME, distance)

    } catch {
      case e: Exception =>
        Index.empty
    }
  }

  /**
   * This function evaluates a distance between two time periods or instants (for periods, it evaluates the minimum distance between their starts/ends).
   *
   * @param timeString1 : String
   * @param timeString2 : String
   * @param limit: Double
   * @param distanceType: String
   * @return Double
   */
  def evaluateDistance(timeString1: String, timeString2: String, limit: Double, distanceType: String): Double = {
    try {
      val period1 = parseTime(timeString1)
      val period2 = parseTime(timeString2)

      //Ensure that periods are well-defined.
      if (period1 == null || period2 == null)
        return Double.PositiveInfinity

      val (start1, end1) = period1
      val (start2, end2) = period2
      val diffInMillisecs = Math.min(Math.min(Math.abs(start1.getTime() - end2.getTime()), Math.abs(end1.getTime() - start2.getTime())), Math.min(Math.abs(start1.getTime() - start2.getTime()), Math.abs(end1.getTime() - end2.getTime())))

      distanceType match {
        case MILLISECS_DISTANCE => diffInMillisecs
        case SECS_DISTANCE => diffInMillisecs / MILLISECS_PER_SEC
        case MINS_DISTANCE => diffInMillisecs / MILLISECS_PER_MIN
        case HOURS_DISTANCE => diffInMillisecs / MILLISECS_PER_HOUR
        case DAYS_DISTANCE => diffInMillisecs / MILLISECS_PER_DAY
        case MONTHS_DISTANCE => diffInMillisecs / MILLISECS_PER_MONTH
        case YEARS_DISTANCE => diffInMillisecs / MILLISECS_PER_YEAR
        case _ => Double.PositiveInfinity
      }

    } catch {
      case e: Exception =>
        Double.PositiveInfinity
    }
  }

  /**
   * This function evaluates a relation between two time periods or instants.
   *
   * @param timeString1 : String
   * @param timeString2 : String
   * @param limit: Double
   * @param relation: String
   * @return Double
   */
  def evaluateRelation(timeString1: String, timeString2: String, limit: Double, relation: String): Double = {
    try {
      val period1 = parseTime(timeString1)
      val period2 = parseTime(timeString2)

      //Ensure that periods are well-defined.
      if (period1 == null || period2 == null)
        return Double.PositiveInfinity

      //Compute the temporal relation.
      if (relate(period1, period2, relation))
        return limit
      else
        return Double.PositiveInfinity

    } catch {
      case e: Exception =>
        Double.PositiveInfinity
    }
  }

  /**
   * This function returns true if the given relation holds between two periods.
   *
   * @param period1: (Date, Date)
   * @param period2: (Date, Date)
   * @param relation: String
   * @return Boolean
   */
  def relate(period1: (Date, Date), period2: (Date, Date), relation: String): Boolean = {
    val (start1, end1) = period1
    val (start2, end2) = period2

    relation match {
      case BEFORE => end1.before(start2)
      case AFTER => start1.after(end2)
      case MEETS => end1.equals(start2)
      case IS_MET_BY => start1.equals(end2)
      case OVERLAPS => end1.after(start2) && end1.before(end2)
      case IS_OVERLAPPED_BY => start1.after(start2) && start1.before(end2)
      case FINISHES => start1.after(start2) && end1.equals(end2)
      case IS_FINISHED_BY => start1.before(start2) && end1.equals(end2)
      case CONTAINS => start1.before(start2) && end1.after(end2)
      case DURRING => start1.after(start2) && end1.before(end2)
      case STARTS => start1.equals(start2) && end1.before(end2)
      case IS_STARTED_BY => start1.equals(start2) && end1.after(end2)
      case EQUALS => start1.equals(start2) && end1.equals(end2)
      case _ => false
    }
  }

}