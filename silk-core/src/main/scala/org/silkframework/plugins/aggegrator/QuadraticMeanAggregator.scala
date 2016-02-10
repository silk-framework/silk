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

package org.silkframework.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.Plugin

/**
 * Computes the weighted quadratic mean.
 */
@Plugin(
  id = "quadraticMean",
  categories = Array("All"),
  label = "Euclidian distance",
  description = "Calculates the Euclidian distance."
)
case class QuadraticMeanAggregator() extends Aggregator {
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (!values.isEmpty) {
      val sqDistance = values.map { case (weight, value) => weight * value * value }.reduceLeft(_ + _)
      val totalWeights = values.map { case (weight, value) => weight }.sum

      Some(math.sqrt(sqDistance / totalWeights))
    }
    else {
      None
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index) = index1 conjunction index2
}