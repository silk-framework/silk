package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.similarity.MultiIndexAggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "min", label = "Minimum", description = "Selects the minimum value.")
class MinimumAggregator() extends MultiIndexAggregator {
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (values.isEmpty) None else Some(values.map(_._2).min)
  }
}