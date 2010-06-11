package de.fuberlin.wiwiss.silk.linkspec

import aggegrator.{MinimumAggregator, MaximumAggregator, AverageAggregator}
import de.fuberlin.wiwiss.silk.util.{Factory, Strategy}

trait Aggregator extends Strategy
{
    def evaluate(weightedValues : Traversable[(Int, Double)]) : Option[Double]
}

object Aggregator extends Factory[Aggregator]
{
    register("average", classOf[AverageAggregator])
    register("max", classOf[MaximumAggregator])
    register("min", classOf[MinimumAggregator])
}
