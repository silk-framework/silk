package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.learning.individual.ComparisonNode

/**
 * A crossover operator which combines the thresholds of two comparisons.
 */
case class ThresholdCrossover() extends NodePairCrossoverOperator[ComparisonNode] {
  def crossover(nodes: SourceTargetPair[ComparisonNode]) = {
    nodes.source.copy(threshold = (nodes.source.threshold + nodes.target.threshold) / 2)
  }
}


