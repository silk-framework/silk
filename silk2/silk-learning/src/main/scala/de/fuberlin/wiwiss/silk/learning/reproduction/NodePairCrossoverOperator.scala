package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{NodeTraverser, Node, LinkageRuleNode}

abstract class NodePairCrossoverOperator[NodeType <: Node : Manifest] extends CrossoverOperator {

  override def apply(nodePair: SourceTargetPair[LinkageRuleNode]): Option[LinkageRuleNode] = {
    //Generate all pairs of compatible nodes
    val sourceNodes = NodeTraverser(nodePair.source).iterateAll.toIndexedSeq
    val targetNodes = NodeTraverser(nodePair.target).iterateAll.toIndexedSeq

    val filteredSourceNodes = sourceNodes.filter(pos => manifest.erasure.isAssignableFrom(pos.node.getClass))
    val filteredTargetNodes = targetNodes.filter(pos => manifest.erasure.isAssignableFrom(pos.node.getClass))

    val nodePairs = for (sourceNode <- filteredSourceNodes; targetNode <- filteredTargetNodes) yield SourceTargetPair(sourceNode, targetNode)

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
  protected def compatible(nodes: SourceTargetPair[NodeType]) = true

  /**
   * Must be overridden in sub classes to execute the crossover operation.
   */
  protected def crossover(nodes: SourceTargetPair[NodeType]): NodeType
}