package de.fuberlin.wiwiss.silk.learning.crossover

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{NodeTraverser, InputNode}

/**
 * A crossover operator which combines the transformations of two comparisons.
 */
case class TransformationCrossover() extends NodePairCrossoverOperator[InputNode] {
  override protected def compatible(nodes: SourceTargetPair[InputNode]) = {
    nodes.source.isSource == nodes.target.isSource
  }

  override protected def crossover(nodePair: SourceTargetPair[InputNode]) = {
    val lowerSourceNodes = NodeTraverser(nodePair.source).iterateAll.withFilter(_.node.isInstanceOf[InputNode]).toIndexedSeq
    val lowerTargetNodes = NodeTraverser(nodePair.target).iterateAll.withFilter(_.node.isInstanceOf[InputNode]).toIndexedSeq

    val lowerSourceNode = lowerSourceNodes(Random.nextInt(lowerSourceNodes.size))
    val lowerTargetNode = lowerTargetNodes(Random.nextInt(lowerTargetNodes.size))

    val updatedLowerNode = lowerTargetNode.update(lowerSourceNode.node)

    val updatedUpperNode = updatedLowerNode.iterate(_.moveUp).toTraversable.last

    updatedUpperNode.node.asInstanceOf[InputNode]
  }
}



