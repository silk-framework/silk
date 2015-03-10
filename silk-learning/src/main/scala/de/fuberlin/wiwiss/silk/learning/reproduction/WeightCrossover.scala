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

package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.{AggregationNode, OperatorNode, ComparisonNode}

/**
 * A crossover operator which combines the weights of two comparisons.
 */
case class WeightCrossover() extends NodePairCrossoverOperator[ComparisonNode] {
  def crossover(nodes: DPair[ComparisonNode]) = {
    nodes.source.copy(weight = (nodes.source.weight + nodes.target.weight) / 2)
  }

  def crossover(nodes: DPair[OperatorNode]) = nodes.source match {
    case c: ComparisonNode => c.copy(weight = (c.weight + nodes.target.weight) / 2)
    case a: AggregationNode => a.copy(weight = (a.weight + nodes.target.weight) / 2)
  }
}