package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.Aggregator

class MinimumAggregator(val params: Map[String, String] = Map()) extends Aggregator
{
    override def evaluate(values : Traversable[(Int, Double)]) =
    {
        if (values.isEmpty) None else Some(values.map(_._2).min)
    }
}