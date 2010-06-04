package de.fuberlin.wiwiss.silk.linkspec.aggegrator

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.linkspec.{Aggregator, Operator}

class MinimumAggregator(val params: Map[String, String] = Map()) extends Aggregator
{
    override def evaluate(values : Traversable[(Int, Double)]) =
    {
        if (values.isEmpty) None else Some(values.map(_._2).min)
    }
}