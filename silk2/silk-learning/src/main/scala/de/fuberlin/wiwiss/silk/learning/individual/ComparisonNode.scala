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

package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{DistanceMeasure, Comparison}

case class ComparisonNode(inputs: DPair[InputNode], threshold: Double, weight: Int, metric: FunctionNode[DistanceMeasure]) extends OperatorNode {
  require(inputs.source.isSource && !inputs.target.isSource, "inputs.source.isSource && !inputs.target.isSource")

  override val children = inputs.source :: inputs.target :: metric :: Nil

  override def updateChildren(newChildren: List[Node]) = {
    val inputNodes = newChildren.collect {
      case c: InputNode => c
    }
    val metricNode = newChildren.collect {
      case c: FunctionNode[DistanceMeasure] => c
    }.head

    ComparisonNode(DPair.fromSeq(inputNodes), threshold, weight, metricNode)
  }

  override def build = {
    Comparison(
      required = false,
      threshold = threshold,
      weight = weight,
      inputs = inputs.map(_.build),
      metric = metric.build()
    )
  }
}

object ComparisonNode {
  def load(comparison: Comparison) = {
    val sourceInputNode = InputNode.load(comparison.inputs.source, true)
    val targetInputNode = InputNode.load(comparison.inputs.target, false)

    val metricNode = FunctionNode.load(comparison.metric, DistanceMeasure)

    ComparisonNode(DPair(sourceInputNode, targetInputNode), comparison.threshold, comparison.weight, metricNode)
  }
}