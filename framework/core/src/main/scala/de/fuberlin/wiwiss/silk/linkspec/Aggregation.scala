package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

class Aggregation(val weight : Int, val operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) =
    {
        val tuples = for (operator <- operators; value <- operator.evaluate(sourceInstance, targetInstance)) yield (operator.weight, value)
        aggregator.evaluate(tuples).toList
    }
}