package de.fuberlin.wiwiss.silk.linkspec.aggegrator

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.linkspec.{Aggregator, Operator}

class AverageAggregator(val params: Map[String, String] = Map()) extends Aggregator
{
    override def evaluate(values : Traversable[(Int, Double)]) =
    {
        var count = values.size
        var result = 0.0
        for (value <- values; val weight <- values.map(_._1); val value <- values.map(_._2))
        {
            result += weight * value
        }
        if (count > 0) Some(result.toDouble/count) else None
    }
}