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

package org.silkframework.rule.evaluation.statistics


/**
 * Statistics about the complexity of multiple linkage rules.
 *
 * @param comparisonCount The number of comparisons in the condition.
 * @param transformationCount The number of transformations in the condition.
 */
case class AggregatedComplexity(comparisonCount: VariableStatistic, transformationCount: VariableStatistic)

/**
 * Aggregates the complexity of multiple linkage rules.
 */
object AggregatedComplexity {
  def apply(complexities: Iterable[LinkageRuleComplexity]): AggregatedComplexity = {
    AggregatedComplexity(
      comparisonCount = VariableStatistic(complexities.map(_.comparisonCount.toDouble)),
      transformationCount = VariableStatistic(complexities.map(_.transformationCount.toDouble))
    )
  }
}