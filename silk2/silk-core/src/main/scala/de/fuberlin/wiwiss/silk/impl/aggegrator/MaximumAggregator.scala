package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.similarity.FlatIndexAggregator
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "max", label = "Maximum", description = "Selects the maximum value.")
class MaximumAggregator() extends FlatIndexAggregator
{
  override def evaluate(values : Traversable[(Int, Double)]) =
  {
    if (values.isEmpty) None else Some(values.map(_._2).max)
  }
}