package de.fuberlin.wiwiss.silk.impl.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.FlatIndexAggregator

class MinimumAggregator(val params: Map[String, String] = Map()) extends FlatIndexAggregator
{
  override def evaluate(values : Traversable[(Int, Double)]) =
  {
    if (values.isEmpty) None else Some(values.map(_._2).min)
  }
}