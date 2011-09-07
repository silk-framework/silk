package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Factory}
import de.fuberlin.wiwiss.silk.learning.individual.LinkageRuleNode

//TODO implement operators: toggle required, change strategy

/**
 * A crossover operator takes a pair of nodes and combines them into a new node.
 */
trait CrossoverOperator extends (SourceTargetPair[LinkageRuleNode] => Option[LinkageRuleNode]) with Strategy {
  /**
   * Applies this crossover operator to a specific pair of nodes.
   */
  def apply(nodePair: SourceTargetPair[LinkageRuleNode]): Option[LinkageRuleNode]

  override def toString = getClass.getSimpleName
}

object CrossoverOperator extends Factory[CrossoverOperator] {
  register(classOf[ThresholdCrossover])
  register(classOf[WeightCrossover])
  register(classOf[AggregationOperatorsCrossover])
  register(classOf[AggregationStrategyCrossover])
  register(classOf[OperatorCrossover])
  register(classOf[TransformationCrossover])
}
