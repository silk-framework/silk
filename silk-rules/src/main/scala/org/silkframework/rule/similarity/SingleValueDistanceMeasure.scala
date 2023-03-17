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

package org.silkframework.rule.similarity

import org.silkframework.entity.Index

import scala.math.min

/**
 * A similarity measure that compares single values (as opposed to sequences of values).
 * If multiple values are provided, all values are compared and the lowest distance is returned.
 */
abstract class SingleValueDistanceMeasure extends DistanceMeasure {

  /**
   * Computes the distance between two strings.
   *
   * @param value1 The first string.
   * @param value2 The second string.
   * @param limit If the expected distance between both strings exceeds this limit, this method may
   *              return Double.PositiveInfinity instead of the actual distance in order to save computation time.
   * @return A positive number that denotes the computed distance between both strings.
   */
  def evaluate(value1: String, value2: String, limit: Double = Double.PositiveInfinity): Double

  /**
    * The empty index, if no value is present.
    */
  def emptyIndex(limit: Double): Index = Index.empty

  /**
   * Computes the index of a single value.
   */
  def indexValue(value: String, limit: Double, sourceOrTarget: Boolean): Index = Index.default

  override def apply(values1: Seq[String], values2: Seq[String], limit: Double): Double = {
    var minDistance = Double.MaxValue

    for (str1 <- values1; str2 <- values2) {
      val distance = evaluate(str1, str2, min(limit, minDistance))
      minDistance = min(minDistance, distance)
      if(minDistance <= 0.0) {
        return minDistance
      }
    }

    minDistance
  }

  override def index(values: Seq[String], limit: Double, sourceOrTarget: Boolean): Index = {
    if(values.isEmpty) {
      emptyIndex(limit)
    } else {
      values.distinct.map((value: String) => indexValue(value, limit, sourceOrTarget)).reduce(_ merge _)
    }
  }
}