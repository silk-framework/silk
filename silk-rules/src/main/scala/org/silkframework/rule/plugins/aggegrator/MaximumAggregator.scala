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
import org.silkframework.rule.similarity.Aggregator
import org.silkframework.runtime.plugin.PluginCategories
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "max",
  categories = Array("All", PluginCategories.recommended),
  label = "Or",
  description = "At least one input score must be within the threshold. Selects the maximum score."
)
case class MaximumAggregator() extends Aggregator {
  /**
   * Returns the maximum of the provided values.
   */
  override def evaluate(values: Traversable[(Int, Double)]): Option[Double] = {
    if (values.isEmpty) {
      None
    } else {
      var max = Double.MinValue
      for(value <- values if value._2 > max) {
        max = value._2
      }
      Some(max)
    }
  }

  /**
   * Combines two indexes into one.
   */
  //TODO change to merge?
  override def combineIndexes(index1: Index, index2: Index): Index = index1 disjunction index2
}
