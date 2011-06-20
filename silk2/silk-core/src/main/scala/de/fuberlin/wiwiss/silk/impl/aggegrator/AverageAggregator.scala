package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.condition.MultiIndexAggregator
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "average", label = "Average", description = "Computes the weighted average.")
class AverageAggregator() extends MultiIndexAggregator
{
  private val positiveWeight : Int = 9
  private val negativeWeight : Int = 10

  override def evaluate(values : Traversable[(Int, Double)]) =
  {
    if(!values.isEmpty)
    {
      var sumWeights = 0
      var sumValues = 0.0

      for((weight, value) <- values)
      {
        if(value > 0.0)
        {
          sumWeights += weight * positiveWeight
          sumValues += weight * positiveWeight * value
        }
        else if(value < 0.0)
        {
          sumWeights += weight * negativeWeight
          sumValues += weight * negativeWeight * value
        }
      }

      val average = sumValues / sumWeights

      Some(average)
    }
    else
    {
      None
    }
  }

  override def computeThreshold(limit : Double, weight : Double) : Double =
  {
    1.0 - ((1.0 - limit) / weight) + positiveWeight.toDouble / negativeWeight
  }
}
