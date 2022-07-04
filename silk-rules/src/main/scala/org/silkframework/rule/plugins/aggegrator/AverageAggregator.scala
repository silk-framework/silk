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
import org.silkframework.rule.similarity.{SimilarityScore, SimpleAggregator, WeightedSimilarityScore}
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.{AggregatorExample, AggregatorExamples, Plugin}

@Plugin(
  id = "average",
  categories = Array(PluginCategories.recommended),
  label = "Average",
  description = "Computes the weighted average."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    inputs = Array(0.4, 0.5, 0.9),
    output = 0.6
  ),
  new AggregatorExample(
    inputs = Array(0.3, 0.5, 0.6),
    weights = Array(1, 1, 2),
    output = 0.5
  ),
  new AggregatorExample(
    description = "Missing scores always lead to an output of none.",
    inputs = Array(-1.0, Double.NaN, 1.0),
    output = Double.NaN
  )
))
case class AverageAggregator() extends SimpleAggregator {
  private val positiveWeight: Int = 1
  private val negativeWeight: Int = 1

  override def evaluate(values: Seq[WeightedSimilarityScore]): SimilarityScore = {
    if (values.nonEmpty) {
      var sumWeights = 0
      var sumValues = 0.0

      for (WeightedSimilarityScore(score, weight) <- values) {
        score match {
          case Some(score) =>
            if (score >= 0.0) {
              sumWeights += weight * positiveWeight
              sumValues += weight * positiveWeight * score
            }
            else if (score < 0.0) {
              sumWeights += weight * negativeWeight
              sumValues += weight * negativeWeight * score
            }
          case None =>
            return SimilarityScore.none
        }
      }

      SimilarityScore(sumValues / sumWeights)
    }
    else {
      SimilarityScore.none
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index): Index = index1 disjunction index2

}
