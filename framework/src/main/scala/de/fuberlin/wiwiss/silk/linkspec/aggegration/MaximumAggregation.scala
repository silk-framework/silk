package de.fuberlin.wiwiss.silk.linkspec.aggegration

import de.fuberlin.wiwiss.silk.linkspec.{Operator, Aggregation}
import de.fuberlin.wiwiss.silk.Instance

class MaximumAggregation(val weight : Int, val operators : Traversable[Operator]) extends Aggregation
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) =
    {
        val values = operators.flatMap(_.evaluate(sourceInstance, targetInstance))
        if (values.isEmpty) Traversable() else Traversable(values.max)
    }
}
