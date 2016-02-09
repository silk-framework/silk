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

import org.silkframework.learning.individual.{InputNode, NodeTraverser}
import org.silkframework.util.DPair

import scala.util.Random

/**
 * A crossover operator which combines the transformations of two comparisons.
 */
case class TransformationCrossover() extends NodePairCrossoverOperator[InputNode] {
  override protected def compatible(nodes: DPair[InputNode]) = {
    nodes.source.isSource == nodes.target.isSource
  }

  override protected def crossover(nodePair: DPair[InputNode]) = {
    val lowerSourceNodes = NodeTraverser(nodePair.source).iterateAll.withFilter(_.node.isInstanceOf[InputNode]).toIndexedSeq
    val lowerTargetNodes = NodeTraverser(nodePair.target).iterateAll.withFilter(_.node.isInstanceOf[InputNode]).toIndexedSeq

    val lowerSourceNode = lowerSourceNodes(Random.nextInt(lowerSourceNodes.size))
    val lowerTargetNode = lowerTargetNodes(Random.nextInt(lowerTargetNodes.size))

    val updatedLowerNode = lowerTargetNode.update(lowerSourceNode.node)

    val updatedUpperNode = updatedLowerNode.iterate(_.moveUp).toTraversable.last

    updatedUpperNode.node.asInstanceOf[InputNode]
  }
}



