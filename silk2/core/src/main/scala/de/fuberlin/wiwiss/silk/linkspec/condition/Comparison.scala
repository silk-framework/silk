package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.instance.Instance
import input.Input

case class Comparison(required : Boolean, weight : Int, inputs : Seq[Input], metric : Metric) extends Operator
{
    require(inputs.size == 2, "Number of inputs must be 2. " + inputs.size + " given.")

    def apply(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double] =
    {
        val set1 = inputs(0).apply(Traversable(sourceInstance, targetInstance))
        val set2 = inputs(1).apply(Traversable(sourceInstance, targetInstance))

        for (str1 <- set1; str2 <- set2) yield metric.evaluate(str1, str2)
    }

    override def toString = metric match
    {
        case Metric(name, params) => "Aggregation(required=" + required + ", weight=" + weight + ", type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
    }
}
