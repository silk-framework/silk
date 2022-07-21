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

import org.silkframework.rule.plugins.aggegrator.HandleMissingValuesAggregator
import org.silkframework.rule.similarity.{Aggregation, Comparison, DistanceMeasure, SimilarityOperator}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.{DPair, IdentifierGenerator}

case class ComparisonNode(inputs: DPair[InputNode], threshold: Double, weight: Int, required: Boolean, metric: FunctionNode[DistanceMeasure]) extends OperatorNode {
  require(inputs.source.isSource && !inputs.target.isSource, "inputs.source.isSource && !inputs.target.isSource")

  override val children = inputs.source :: inputs.target :: metric :: Nil

  override def updateChildren(newChildren: List[Node]) = {
    val sourceInput = newChildren.collect{ case c: InputNode if c.isSource => c }.head
    val targetInput = newChildren.collect{ case c: InputNode if !c.isSource => c }.head
    val metricNode = newChildren.collect{ case c: FunctionNode[DistanceMeasure] @unchecked => c }.head

    ComparisonNode(DPair(sourceInput, targetInput), threshold, weight, required, metricNode)
  }

  override def build(implicit identifiers: IdentifierGenerator): SimilarityOperator = {
    val comparison =
      Comparison(
        identifiers.generate(metric.id),
        threshold = threshold,
        weight = weight,
        inputs = inputs.map(_.build),
        metric = metric.build
      )
    if(required) {
      comparison
    } else {
      Aggregation(
        identifiers.generate(metric.id + "_optional"),
        aggregator = HandleMissingValuesAggregator(defaultValue = 1.0),
        operators = Seq(comparison)
      )
    }
  }
}

object ComparisonNode {
  def load(comparison: Comparison)(implicit context: PluginContext): ComparisonNode = {
    val sourceInputNode = InputNode.load(comparison.inputs.source, true)
    val targetInputNode = InputNode.load(comparison.inputs.target, false)

    val metricNode = FunctionNode.load(comparison.metric, DistanceMeasure)

    ComparisonNode(DPair(sourceInputNode, targetInputNode), comparison.threshold, comparison.weight, required = true, metricNode)
  }
}