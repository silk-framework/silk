package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.FlatIndexAggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "max", label = "Maximum", description = "Selects the maximum value.")
class MaximumAggregator() extends FlatIndexAggregator {
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (values.isEmpty) None else Some(values.map(_._2).max)
  }
}