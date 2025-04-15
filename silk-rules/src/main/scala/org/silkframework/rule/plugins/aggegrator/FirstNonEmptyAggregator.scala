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
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "firstNonEmpty",
  categories = Array(PluginCategories.recommended),
  label = "First non-empty score",
  description = "Forwards the first input that provides a non-empty similarity score."
)
@AggregatorExamples(Array(
  new AggregatorExample(
    description = "The first defined score is returned, even if it's not the highest score.",
    inputs = Array(Double.NaN, 0.2, 0.5),
    output = 0.2
  )
))
case class FirstNonEmptyAggregator() extends SimpleAggregator {

  override def evaluate(values: Seq[WeightedSimilarityScore]): SimilarityScore = {
    values.find(_.score.isDefined).map(_.unweighted).getOrElse(SimilarityScore.none)
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index): Index = index1 merge index2

}
