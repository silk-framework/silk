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

package org.silkframework.learning.individual

import org.silkframework.config.Prefixes
import org.silkframework.rule.plugins.aggegrator.HandleMissingValuesAggregator
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.util.IdentifierGenerator

trait OperatorNode extends Node {

  def weight: Int

  def build(implicit identifiers: IdentifierGenerator): SimilarityOperator
}

object OperatorNode {
  def load(operator: SimilarityOperator)(implicit pluginContext: PluginContext): OperatorNode = operator match {
    case Aggregation(_, _, HandleMissingValuesAggregator(1.0), Seq(comparison: Comparison)) =>
      ComparisonNode.load(comparison).copy(required = false)
    case aggregation: Aggregation =>
      AggregationNode.load(aggregation)
    case comparison: Comparison =>
      ComparisonNode.load(comparison)
  }
}