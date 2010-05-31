package de.fuberlin.wiwiss.silk.linkspec.aggegration

import de.fuberlin.wiwiss.silk.linkspec.{Operator, Aggregation}
import de.fuberlin.wiwiss.silk.Instance

class AverageAggregation(val weight : Int, val operators : Traversable[Operator]) extends Aggregation
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) =
    {
        var count = 0
        var result = 0.0
        for (operator <- operators; val operatorValues = operator.evaluate(sourceInstance, targetInstance); operatorValue <- operatorValues)
        {
            result += operator.weight * operatorValue
            count += 1
        }
        if (count > 0) Traversable(result.toDouble/count) else Traversable()
    }
}