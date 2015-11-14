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

import java.util.Date
import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.entity.Index

/**
 * Useful utils for the temporal plugins of Silk.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

object Utils {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
   * This function indexes times. Assumes that all the times are between "the epoch" (January 1, 1970, 00:00:00 GMT) and "NOW".
   *
   * @param timeString : String
   * @param blockingParameter: Double
   * @param distance: Double (optional; for distance measures)
   * @param distanceType: String (optional; for distance measures)
   * @return Index
   */
  def indexTimes(timeString: String, blockingParameter: Double, distance: Double = 0.0, distanceType: String = Constants.MILLISECS_DISTANCE): Index = {
    try {
      val period = Parser.parseTime(timeString)

      //Ensure that period is well-defined.
      if (period == null)
        return Index.empty

      val (start, end) = period
          
      val divisor = { 
        distanceType match {
          case Constants.MILLISECS_DISTANCE => 1.0
          case Constants.SECS_DISTANCE => Constants.MILLISECS_PER_SEC
          case Constants.MINS_DISTANCE => Constants.MILLISECS_PER_MIN
          case Constants.HOURS_DISTANCE => Constants.MILLISECS_PER_HOUR
          case Constants.DAYS_DISTANCE => Constants.MILLISECS_PER_DAY
          case Constants.MONTHS_DISTANCE => Constants.MILLISECS_PER_MONTH
          case Constants.YEARS_DISTANCE => Constants.MILLISECS_PER_YEAR
          case _ => Double.PositiveInfinity
        }  
      }
            
      val blockCount = (Constants.TIME_RANGE/divisor).toInt
      val minblock = ((start.getTime()-distance)/divisor*blockingParameter).toInt
      val maxblock = ((end.getTime()+distance)/divisor*blockingParameter).toInt
      val blocks = for (i <- minblock to maxblock) yield i

      Index.oneDim(blocks.toSet, blockCount)

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
      val period1 = Parser.parseTime(timeString1)
      val period2 = Parser.parseTime(timeString2)

      //Ensure that periods are well-defined.
      if (period1 == null || period2 == null)
        return Double.PositiveInfinity

      val (start1, end1) = period1
      val (start2, end2) = period2
      val diffInMillisecs = Math.min(Math.min(Math.abs(start1.getTime() - end2.getTime()), Math.abs(end1.getTime() - start2.getTime())), Math.min(Math.abs(start1.getTime() - start2.getTime()), Math.abs(end1.getTime() - end2.getTime())))

      distanceType match {
        case Constants.MILLISECS_DISTANCE => diffInMillisecs
        case Constants.SECS_DISTANCE => diffInMillisecs / Constants.MILLISECS_PER_SEC
        case Constants.MINS_DISTANCE => diffInMillisecs / Constants.MILLISECS_PER_MIN
        case Constants.HOURS_DISTANCE => diffInMillisecs / Constants.MILLISECS_PER_HOUR
        case Constants.DAYS_DISTANCE => diffInMillisecs / Constants.MILLISECS_PER_DAY
        case Constants.MONTHS_DISTANCE => diffInMillisecs / Constants.MILLISECS_PER_MONTH
        case Constants.YEARS_DISTANCE => diffInMillisecs / Constants.MILLISECS_PER_YEAR
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
   * @param relation: String
   * @return 0.0, if the relation is true. 1.0, otherwise.
   */
  def evaluateRelation(timeString1: String, timeString2: String, relation: String): Double = {
    try {
      val period1 = Parser.parseTime(timeString1)
      val period2 = Parser.parseTime(timeString2)

      //Ensure that periods are well-defined.
      if (period1 == null || period2 == null)
        return 1.0

      //Compute the temporal relation.
      if (relate(period1, period2, relation))
        return 0.0
      else
        return 1.0

    } catch {
      case e: Exception =>
        1.0
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
      case Constants.BEFORE => end1.before(start2)
      case Constants.AFTER => start1.after(end2)
      case Constants.MEETS => end1.equals(start2)
      case Constants.IS_MET_BY => start1.equals(end2)
      case Constants.OVERLAPS => end1.after(start2) && end1.before(end2)
      case Constants.IS_OVERLAPPED_BY => start1.after(start2) && start1.before(end2)
      case Constants.FINISHES => start1.after(start2) && end1.equals(end2)
      case Constants.IS_FINISHED_BY => start1.before(start2) && end1.equals(end2)
      case Constants.CONTAINS => start1.before(start2) && end1.after(end2)
      case Constants.DURING => start1.after(start2) && end1.before(end2)
      case Constants.STARTS => start1.equals(start2) && end1.before(end2)
      case Constants.IS_STARTED_BY => start1.equals(start2) && end1.after(end2)
      case Constants.EQUALS => start1.equals(start2) && end1.equals(end2)
      case _ => false
    }
  }

}