package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.Aggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(id = "min", label = "Minimum", description = "Selects the minimum value.")
case class MinimumAggregator() extends Aggregator {
  /**
   * Returns the minimum of the provided values.
   */
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (values.isEmpty)
      None
    else {
      var min = Double.MaxValue
      for(value <- values) {
        if(value._2 < min)
          min = value._2
      }
      Some(min)
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 conjunction index2
}