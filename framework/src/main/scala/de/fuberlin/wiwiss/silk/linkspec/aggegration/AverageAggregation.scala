package de.fuberlin.wiwiss.silk.linkspec.aggegration

import de.fuberlin.wiwiss.silk.linkspec.{Operator, Aggregation}
import de.fuberlin.wiwiss.silk.Instance

class AverageAggregation(val weight : Int, operators : Traversable[Operator]) extends Aggregation
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) =  Traversable(0.0)
}