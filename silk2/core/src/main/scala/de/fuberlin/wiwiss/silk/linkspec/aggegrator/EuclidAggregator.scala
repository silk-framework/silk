package de.fuberlin.wiwiss.silk.linkspec.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.Aggregator

/**
 * Computes the weighted euclidean distance.
 */
class EuclidAggregator(val params: Map[String, String] = Map.empty) extends Aggregator
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
