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

package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.rule.similarity.Aggregator
import scala.math._
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

/**
 * Computes the weighted geometric mean.
 */
@Plugin(
  id = "geometricMean",
  categories = Array("All"),
  label = "Geometric mean",
  description = "Compute the (weighted) geometric mean."
)
case class GeometricMeanAggregator() extends Aggregator {
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (!values.isEmpty) {
      val weightedProduct = values.map { case (weight, value) => pow(value, weight) }.reduceLeft(_ * _)
      val totalWeights = values.map { case (weight, value) => weight }.sum

      Some(pow(weightedProduct, 1.0 / totalWeights))
    }
    else {
      None
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 conjunction index2
}