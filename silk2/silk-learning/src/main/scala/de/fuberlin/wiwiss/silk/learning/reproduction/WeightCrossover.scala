package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.learning.individual.ComparisonNode
import de.fuberlin.wiwiss.silk.util.DPair

/**
 * A crossover operator which combines the weights of two comparisons.
 */
case class WeightCrossover() extends NodePairCrossoverOperator[ComparisonNode] {
  def crossover(nodes: DPair[ComparisonNode]) = {
    nodes.source.copy(weight = (nodes.source.weight + nodes.target.weight) / 2)
  }
}