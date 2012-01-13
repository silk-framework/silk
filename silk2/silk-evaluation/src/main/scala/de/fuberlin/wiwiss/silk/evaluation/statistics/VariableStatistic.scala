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

package de.fuberlin.wiwiss.silk.evaluation.statistics

import math.abs

/**
 * Statistics about a stochastic variable such as the fMeasure of a set of linkage rules.
 */
case class VariableStatistic(mean: Double, standardDeviation: Double) {
  /**
   * Transforms this statistics by applying a function to all measures.
   */
  def map(f: Double => Double) = {
    VariableStatistic(f(mean), f(standardDeviation))
  }

  /**
   * Formats this variable statistics as a string.
   *
   * @param precision The number of digits after the decimal separator
   * @param includeDeviation Include the standard deviation
   */
  def format(precision: Int = 3, includeDeviation: Boolean = true) = {
    def meanStr = ("%." + precision + "f").format(mean)
    def stdStr  = ("%." + precision + "f").format(standardDeviation)

    if(includeDeviation)
      meanStr + " (" + stdStr + ")"
    else
      meanStr
  }
  
  override def toString = format()
}

/**
 * Generates the statistics for variable.
 */
object VariableStatistic {
  /**
   * Generates the statistics for variable based on a sample of values.
   */
  def apply(values: Traversable[Double]): VariableStatistic = {
    val mean = values.sum / values.size
    val standardDeviation = values.map(x => abs(x - mean)).sum / values.size

    VariableStatistic(mean, standardDeviation)
  }
}