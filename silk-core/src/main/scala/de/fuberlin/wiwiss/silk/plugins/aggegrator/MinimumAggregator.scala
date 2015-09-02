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
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(
  id = "min",
  categories = Array("All", "Recommended"),
  label = "Minimum",
  description = "Selects the minimum value."
)
case class MinimumAggregator() extends Aggregator {
  /**
   * Returns the minimum of the provided values.
   */
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (values.isEmpty)
      None
    else {
      var min = Double.MaxValue
      for(value <- values) {
        if(value._2 < min)
          min = value._2
      }
      Some(min)
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 conjunction index2
}