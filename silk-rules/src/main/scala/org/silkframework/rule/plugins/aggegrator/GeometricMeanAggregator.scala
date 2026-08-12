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

package org.silkframework.rule.plugins.aggegrator

import org.silkframework.entity.Index
import org.silkframework.rule.annotations.{AggregatorExample, AggregatorExamples}
import org.silkframework.rule.similarity.{SimilarityScore, SimpleAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.annotations.Plugin

import scala.math._

/**
 * Computes the weighted geometric mean.
 */
@Plugin(
  id = "geometricMean",
  categories = Array(),
  label = "Geometric mean",
  description = "Compute the (weighted) geometric mean."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    inputs = Array(0.0, 0.0, 0.0),
    weights = Array(1, 2, 1),
    output = 0.0
  ),
  new AggregatorExample(
    inputs = Array(1.0, 1.0, 1.0),
    weights = Array(1, 2, 1),
    output = 1.0
  ),
  new AggregatorExample(
    inputs = Array(0.5, 1.0),
    weights = Array(2, 1),
    output = 0.629961
  ),
  new AggregatorExample(
    inputs = Array(0.5, 1.0, 0.7),
    weights = Array(2, 1, 5),
    output = 0.672866
  ),
  new AggregatorExample(
    inputs = Array(0.1, 0.9, 0.2),
    weights = Array(10, 2, 3),
    output = 0.153971
  ),
  new AggregatorExample(
    description = "Missing scores always lead to an output of none.",
    inputs = Array(-1.0, Double.NaN, 1.0),
    output = Double.NaN
  ),
  new AggregatorExample(
    description = "If any score is negative (a definite mismatch), the lowest score is returned instead of a link-generating 0.",
    inputs = Array(-1.0, 1.0),
    output = -1.0
  ),
  new AggregatorExample(
    description = "The lowest of several negative scores is returned.",
    inputs = Array(-0.2, -0.5, 1.0),
    output = -0.5
  )
))
case class GeometricMeanAggregator() extends SimpleAggregator {

  override def evaluate(values: Seq[WeightedSimilarityScore]): SimilarityScore = {
    if (values.nonEmpty) {
      var sumWeights = 0
      var weightedProduct = 1.0
      var minScore = 1.0

      for (WeightedSimilarityScore(score, weight) <- values) {
        score match {
          case Some(score) =>
            sumWeights += weight
            minScore = min(minScore, score)
            // Clamped to 0: the geometric mean is undefined for negative values and would yield NaN.
            weightedProduct *= pow(max(score, 0.0), weight)
          case None =>
            return SimilarityScore.none
        }
      }

      if (minScore < 0.0) {
        // A definite mismatch must stay negative instead of producing a link-generating 0.
        SimilarityScore(minScore)
      } else {
        SimilarityScore(pow(weightedProduct, 1.0 / sumWeights))
      }
    }
    else {
      SimilarityScore.none
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 conjunction index2
}