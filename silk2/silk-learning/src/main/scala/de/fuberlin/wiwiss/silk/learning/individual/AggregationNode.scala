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

import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregator, Aggregation}

case class AggregationNode(aggregation: String, weight: Int, required: Boolean, operators: List[OperatorNode]) extends OperatorNode {
  override val children = operators

  override def updateChildren(children: List[Node]) = {
    AggregationNode(aggregation, weight, required, children.map(_.asInstanceOf[OperatorNode]))
  }

  def build: Aggregation = {
    Aggregation(
      required = required,
      weight = weight,
      operators = operators.map(_.build),
      aggregator = Aggregator(aggregation, Map.empty)
    )
  }
}

object AggregationNode {
  def load(aggregation: Aggregation) = {
    val aggregatorId = aggregation.aggregator match {
      case Aggregator(id, _) => id
    }

    val operatorNodes = aggregation.operators.map(OperatorNode.load).toList

    AggregationNode(aggregatorId, aggregation.weight, aggregation.required, operatorNodes)
  }
}