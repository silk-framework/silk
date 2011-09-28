package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.Aggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.Index

@Plugin(id = "max", label = "Maximum", description = "Selects the maximum value.")
class MaximumAggregator() extends Aggregator {
  /**
   * Returns the maximum of the provided values.
   */
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (values.isEmpty) None else Some(values.map(_._2).max)
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 disjunction index2
}