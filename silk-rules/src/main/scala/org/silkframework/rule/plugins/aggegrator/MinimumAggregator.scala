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

import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.similarity.{Aggregator, SimilarityOperator, SimilarityScore}
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.{AggregatorExample, AggregatorExamples, Plugin}
import org.silkframework.util.DPair

@Plugin(
  id = "min",
  categories = Array(PluginCategories.recommended),
  label = "And",
  description = "All input scores must be within the threshold. Selects the minimum score."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    inputs = Array(1.0, 0.0),
    output = 0.0
  ),
  new AggregatorExample(
    inputs = Array(-1.0, 0.0, 0.5, 1.0),
    output = -1.0
  ),
  new AggregatorExample(
    description = "Missing scores default to a similarity score of -1.",
    inputs = Array(1.0, Double.NaN, -0.5),
    output = -1.0
  ),
  new AggregatorExample(
    description = "Weights are ignored.",
    inputs = Array(1.0, 0.0),
    weights = Array(1000, 0),
    output = 0.0
  )
))
case class MinimumAggregator() extends Aggregator {
  /**
   * Returns the minimum of the provided values.
   */
  override def apply(operators: Seq[SimilarityOperator], entities: DPair[Entity], limit: Double): SimilarityScore = {
    var minScore = Double.MaxValue
    if(operators.isEmpty) {
      return SimilarityScore.none
    }
    for(operator <- operators) {
      operator(entities, limit) match {
        case Some(s) if s >= limit =>
          minScore = math.min(minScore, s)
        case _ =>
          return SimilarityScore(-1.0)
      }
    }
    SimilarityScore(minScore)
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index): Index = index1 conjunction index2
}
