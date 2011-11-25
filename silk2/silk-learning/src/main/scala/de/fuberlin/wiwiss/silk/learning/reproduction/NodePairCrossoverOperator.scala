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
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{NodeTraverser, Node, LinkageRuleNode}

abstract class NodePairCrossoverOperator[NodeType <: Node : Manifest] extends CrossoverOperator {

  override def apply(nodePair: DPair[LinkageRuleNode]): Option[LinkageRuleNode] = {
    //Generate all pairs of compatible nodes
    val sourceNodes = NodeTraverser(nodePair.source).iterateAll.toIndexedSeq
    val targetNodes = NodeTraverser(nodePair.target).iterateAll.toIndexedSeq

    val filteredSourceNodes = sourceNodes.filter(pos => manifest.erasure.isAssignableFrom(pos.node.getClass))
    val filteredTargetNodes = targetNodes.filter(pos => manifest.erasure.isAssignableFrom(pos.node.getClass))

    val nodePairs = for (sourceNode <- filteredSourceNodes; targetNode <- filteredTargetNodes) yield DPair(sourceNode, targetNode)

    //Filter pairs which are compatible with the crossover operator
    val compatiblePairs = nodePairs.filter(pair => compatible(pair.map(_.node.asInstanceOf[NodeType])))

    if (compatiblePairs.size == 0) {
      None
    }
    else {
      //Choose a random pair
      val crossoverPair = compatiblePairs(Random.nextInt(compatiblePairs.size))

      //Apply the crossover operator
      val updatedNode = crossover(crossoverPair.map(_.node.asInstanceOf[NodeType]))

      //Update linkage rule node
      val linkageRule = crossoverPair.source.update(updatedNode).root.node.asInstanceOf[LinkageRuleNode]

      Some(linkageRule)
    }
  }

  /**
   * Determines if the operator can be applied to a specific pair of nodes.
   */
  protected def compatible(nodes: DPair[NodeType]) = true

  /**
   * Must be overridden in sub classes to execute the crossover operation.
   */
  protected def crossover(nodes: DPair[NodeType]): NodeType
}