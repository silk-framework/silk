package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.ComparisonNode

/**
 * A crossover operator which combines the thresholds of two comparisons.
 */
case class ThresholdCrossover() extends NodePairCrossoverOperator[ComparisonNode] {
  def crossover(nodes: DPair[ComparisonNode]) = {
    nodes.source.copy(threshold = (nodes.source.threshold + nodes.target.threshold) / 2)
  }
}


