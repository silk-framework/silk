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

package org.silkframework.learning.reproduction

import org.silkframework.learning.individual.OperatorNode
import org.silkframework.util.DPair

/**
 * A crossover operator which combines the operators of two aggregations.
 */
case class OperatorCrossover() extends NodePairCrossoverOperator[OperatorNode] {
  override protected def crossover(nodes: DPair[OperatorNode]) = {
    nodes.target
  }
}




