package de.fuberlin.wiwiss.silk.learning.crossover

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.OperatorNode

/**
 * A crossover operator which combines the operators of two aggregations.
 */
case class OperatorCrossover() extends NodePairCrossoverOperator[OperatorNode] {
  override protected def crossover(nodes: SourceTargetPair[OperatorNode]) = {
    nodes.target
  }
}




