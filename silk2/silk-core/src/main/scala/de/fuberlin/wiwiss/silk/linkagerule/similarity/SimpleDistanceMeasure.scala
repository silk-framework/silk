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

package de.fuberlin.wiwiss.silk.linkagerule.similarity

import math.min
import de.fuberlin.wiwiss.silk.entity.Index

/**
 * A simple similarity measure which compares pairs of values.
 */
abstract class SimpleDistanceMeasure extends DistanceMeasure {
  override final def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    var minDistance = Double.MaxValue

    for (str1 <- values1; str2 <- values2 if minDistance > 0.0) {
      val distance = evaluate(str1, str2, min(limit, minDistance))
      minDistance = min(minDistance, distance)
    }

    minDistance
  }

  override final def index(values: Set[String], limit: Double): Index = {
    if(values.isEmpty)
      Index.empty
    else
      values.map(indexValue(_, limit)).reduce(_ merge _)
  }

  /**
   * Computes the similarity of a pair of values.
   */
  def evaluate(value1: String, value2: String, limit: Double = Double.PositiveInfinity): Double

  /**
   * Computes the index of a single value.
   */
  def indexValue(value: String, limit: Double): Index = Index.default
}