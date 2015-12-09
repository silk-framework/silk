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

import math.min
import org.silkframework.entity.Index

/**
 * A simple similarity measure, which compares pairs of values.
 */
abstract class SimpleDistanceMeasure extends DistanceMeasure {

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
   * Computes the index of a single value.
   */
  def indexValue(value: String, limit: Double): Index = Index.default

  override final def apply(values1: Seq[String], values2: Seq[String], limit: Double): Double = {
    var minDistance = Double.MaxValue

    for (str1 <- values1; str2 <- values2 if minDistance > 0.0) {
      val distance = evaluate(str1, str2, min(limit, minDistance))
      minDistance = min(minDistance, distance)
    }

    minDistance
  }

  override final def index(values: Seq[String], limit: Double): Index = {
    if(values.isEmpty)
      indexValue("", limit) //We index an empty value, so that the index is empty but has the right size
    else
      values.distinct.map(indexValue(_, limit)).reduce(_ merge _)
  }
}