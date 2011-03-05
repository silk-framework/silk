package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.condition.MultiIndexAggregator
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "average", label = "Average", description = "Computes the weighted average.")
class AverageAggregator() extends MultiIndexAggregator
{
  override def evaluate(values : Traversable[(Int, Double)]) =
  {
    if(!values.isEmpty)
    {
      var sumWeights = 0
      var sumValues = 0.0

      for((weight, value) <- values)
      {
        sumWeights += weight
        sumValues += weight * value
      }

      Some(sumValues / sumWeights)
    }
    else
    {
      None
    }
  }
}
