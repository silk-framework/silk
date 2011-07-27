package de.fuberlin.wiwiss.silk.learning.crossover

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.ComparisonNode

/**
 * A crossover operator which combines the limits of two comparisons.
 */
case class LimitCrossover() extends NodePairCrossoverOperator[ComparisonNode] {
  def crossover(nodes: SourceTargetPair[ComparisonNode]) = {
    nodes.source.copy(threshold = (nodes.source.threshold + nodes.target.threshold) / 2)
  }
}


