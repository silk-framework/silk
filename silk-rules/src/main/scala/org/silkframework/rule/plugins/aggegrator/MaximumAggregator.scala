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
  id = "max",
  categories = Array(PluginCategories.recommended),
  label = "Or",
  description = "At least one input score must be within the threshold. Selects the maximum score."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    inputs = Array(0.5, 0.0),
    output = 0.5
  ),
  new AggregatorExample(
    inputs = Array(-1.0, -0.5, -0.3),
    output = -0.3
  ),
  new AggregatorExample(
    description = "Missing scores default to a similarity score of -1.",
    inputs = Array(Double.NaN),
    output = -1.0
  ),
  new AggregatorExample(
    description = "Weights are ignored.",
    inputs = Array(1.0, 0.0),
    weights = Array(1000, 0),
    output = 1.0
  )
))
case class MaximumAggregator() extends SimpleAggregator {
  /**
   * Returns the maximum of the provided values.
   */
  override def evaluate(values: Seq[WeightedSimilarityScore]): SimilarityScore = {
    if (values.isEmpty) {
      SimilarityScore.none
    } else {
      var maxScore = Double.MinValue
      for(value <- values) {
        maxScore = math.max(maxScore, value.score.getOrElse(-1.0))
      }
      SimilarityScore(maxScore)
    }
  }

  /**
   * Combines two indexes into one.
   */
  //TODO change to merge?
  override def combineIndexes(index1: Index, index2: Index): Index = index1 disjunction index2
}
