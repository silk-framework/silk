package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance

case class Aggregation(required : Boolean, weight : Int, operators : Traversable[Operator], aggregator : Aggregator) extends Operator
{
    override def apply(sourceInstance : Instance, targetInstance : Instance) =
    {
        val tuples = for (operator <- operators; value <- operator(sourceInstance, targetInstance)) yield (operator.weight, value)
        aggregator.evaluate(tuples).toList
    }

    override def toString = aggregator match
    {
        case Aggregator(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", operators=" + operators + ")"
    }
}
