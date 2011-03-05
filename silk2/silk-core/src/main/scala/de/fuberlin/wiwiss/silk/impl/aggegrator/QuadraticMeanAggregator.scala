package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.condition.MultiIndexAggregator
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

/**
 * Computes the weighted quadratic mean.
 */
@StrategyAnnotation(id = "quadraticMean", label = "Euclidian distance", description = "Calculates the Euclidian distance.")
class QuadraticMeanAggregator() extends MultiIndexAggregator
{
  override def evaluate(values : Traversable[(Int, Double)]) =
  {
    if(!values.isEmpty)
    {
      val sqDistance = values.map{case (weight, value) => weight * value * value}.reduceLeft(_ + _)
      val totalWeights = values.map{case (weight, value) => weight}.sum

      Some(math.sqrt(sqDistance / totalWeights))
    }
    else
    {
      None
    }
  }
}