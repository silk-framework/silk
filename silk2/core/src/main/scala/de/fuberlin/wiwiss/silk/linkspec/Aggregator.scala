package de.fuberlin.wiwiss.silk.linkspec

import aggegrator.{MinimumAggregator, MaximumAggregator, AverageAggregator}

trait Aggregator
{
    val params : Map[String, String]

    def evaluate(weightedValues : Traversable[(Int, Double)]) : Option[Double]
}

object Aggregator
{
    def apply(aggType : String, params : Map[String, String]) : Aggregator =
    {
        aggType match
        {
            case "average" => new AverageAggregator(params)
            case "max" => new MaximumAggregator(params)
            case "min" => new MinimumAggregator(params)
            case _ => throw new IllegalArgumentException("Aggregation type unknown: " + aggType)
        }
    }
}