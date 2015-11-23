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

import org.silkframework.util.DPair
import org.silkframework.learning.individual.{FunctionNode, InputNode, Node}

case class SubtreeCrossover() extends NodePairCrossoverOperator[Node] {

  override protected def compatible(nodes: DPair[Node]) = {
    nodes match {
      case DPair(i1: InputNode, i2: InputNode) =>
        i1.isSource == i2.isSource
      case DPair(n1: FunctionNode[_], n2: FunctionNode[_]) =>
        n1.factory.getClass == n2.factory.getClass
      case _ =>
        nodes.source.getClass == nodes.target.getClass
    }
  }
  
  override protected def crossover(nodes: DPair[Node]): Node = {
    nodes.target
  }
}