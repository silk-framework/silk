package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.learning.individual.AggregationNode

/**
 * A crossover operator which interchanges the aggregation strategies.
 */
case class AggregationStrategyCrossover() extends NodePairCrossoverOperator[AggregationNode] {
  override protected def crossover(nodes: DPair[AggregationNode]) = {
    nodes.source.copy(aggregation = nodes.target.aggregation)
  }
}
