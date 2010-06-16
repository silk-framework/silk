package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

case class Aggregation(weight : Int, operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
    override def evaluate(sourceInstance : Instance, targetInstance : Instance) =
    {
        val tuples = for (operator <- operators; value <- operator.evaluate(sourceInstance, targetInstance)) yield (operator.weight, value)
        aggregator.evaluate(tuples).toList
    }

    override def toString = aggregator match
    {
        case Aggregator(name, params) => "Aggregation(weight=" + weight + ", type=" + name + ", params=" + params + ", operators=" + operators + ")"
    }
}
