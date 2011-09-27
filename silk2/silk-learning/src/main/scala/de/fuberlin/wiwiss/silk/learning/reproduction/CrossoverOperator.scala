package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.util.plugin.{AnyPlugin, PluginFactory}
import de.fuberlin.wiwiss.silk.learning.individual.LinkageRuleNode

//TODO implement operators: toggle required, change plugin

/**
 * A crossover operator takes a pair of nodes and combines them into a new node.
 */
trait CrossoverOperator extends (DPair[LinkageRuleNode] => Option[LinkageRuleNode]) with AnyPlugin {
  /**
   * Applies this crossover operator to a specific pair of nodes.
   */
  def apply(nodePair: DPair[LinkageRuleNode]): Option[LinkageRuleNode]

  override def toString = getClass.getSimpleName
}

object CrossoverOperator extends PluginFactory[CrossoverOperator] {
  register(classOf[ThresholdCrossover])
  register(classOf[WeightCrossover])
  register(classOf[AggregationOperatorsCrossover])
  register(classOf[AggregationStrategyCrossover])
  register(classOf[OperatorCrossover])
  register(classOf[TransformationCrossover])
}
