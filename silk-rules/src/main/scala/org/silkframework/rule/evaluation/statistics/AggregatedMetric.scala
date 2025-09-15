/* 
 * Copyright 2009-2011 Freie Universität Berlin
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

package org.silkframework.rule.evaluation.statistics

import java.util.Locale
import scala.math.{abs, sqrt}

/**
 * Statistics about a stochastic variable such as the fMeasure of a set of linkage rules.
 */
case class AggregatedMetric(mean: Double, standardDeviation: Double) {
  /**
   * Formats this variable statistics as a string.
   *
   * @param precision The number of digits after the decimal separator
   * @param includeDeviation Include the standard deviation
   */
  def format(precision: Int = 3, includeDeviation: Boolean = true): String = {
    def meanStr = (s"%." + precision + "f").formatLocal(Locale.US, mean)
    def stdStr  = ("%." + precision + "f").formatLocal(Locale.US, standardDeviation)

    if(includeDeviation) {
      meanStr + " ± " + stdStr
    } else {
      meanStr
    }
  }

  override def toString: String = format()
}

/**
 * Generates the statistics for variable.
 */
object AggregatedMetric {
  /**
   * Generates the statistics for variable based on a sample of values.
   */
  def apply(values: Iterable[Double]): AggregatedMetric = {
    val mean = values.sum / values.size
    val variance = values.map(x => math.pow(x - mean, 2)).sum / values.size
    val standardDeviation = sqrt(variance)

    AggregatedMetric(mean, standardDeviation)
  }

  /**
   * Averages multiple metrics into one.
   * This is useful for metrics that are averaged, such as precision or recall.
   */
  def average(metrics: Iterable[AggregatedMetric]): AggregatedMetric = {
    val combinedMean = metrics.map(_.mean).sum / metrics.size
    val combinedVariance = metrics.map(m => m.standardDeviation * m.standardDeviation).sum / metrics.size
    val combinedStandardDeviation = sqrt(combinedVariance)
    AggregatedMetric(combinedMean, combinedStandardDeviation)
  }

  /**
   * Sums multiple metrics into one.
   * This is useful for metrics that can be summed, such as the total runtime.
   */
  def sum(metrics: Iterable[AggregatedMetric]): AggregatedMetric = {
    val combinedMean = metrics.map(_.mean).sum
    val combinedVariance = metrics.map(m => m.standardDeviation * m.standardDeviation).sum
    val combinedStandardDeviation = sqrt(combinedVariance)
    AggregatedMetric(combinedMean, combinedStandardDeviation)
  }
}