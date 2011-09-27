package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.OperatorNode

/**
 * A crossover operator which combines the operators of two aggregations.
 */
case class OperatorCrossover() extends NodePairCrossoverOperator[OperatorNode] {
  override protected def crossover(nodes: DPair[OperatorNode]) = {
    nodes.target
  }
}




