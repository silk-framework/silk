package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.Aggregator

/**
 * Computes the weighted quadratic mean.
 */
class QuadraticMeanAggregator(val params: Map[String, String] = Map.empty) extends Aggregator
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