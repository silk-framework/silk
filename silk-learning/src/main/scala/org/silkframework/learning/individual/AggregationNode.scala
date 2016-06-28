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
import org.silkframework.rule.similarity.{Aggregation, Aggregator}
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.util.IdentifierGenerator

case class AggregationNode(aggregation: String, weight: Int, required: Boolean, operators: List[OperatorNode]) extends OperatorNode {
  override val children = operators

  override def updateChildren(children: List[Node]) = {
    AggregationNode(aggregation, weight, required, children.map(_.asInstanceOf[OperatorNode]))
  }

  def build(implicit identifiers: IdentifierGenerator): Aggregation = {
    Aggregation(
      identifiers.generate(aggregation),
      required = required,
      weight = weight,
      operators = operators.map(_.build),
      aggregator = Aggregator(aggregation, Map.empty)(Prefixes.empty, EmptyResourceManager)
    )
  }
}

object AggregationNode {
  def load(aggregation: Aggregation) = {
    val aggregatorId = aggregation.aggregator match {
      case Aggregator(plugin, _) => plugin.id
    }

    val operatorNodes = aggregation.operators.map(OperatorNode.load).toList

    AggregationNode(aggregatorId, aggregation.weight, aggregation.required, operatorNodes)
  }
}