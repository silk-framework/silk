package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

case class Comparison(weight : Int, inputs : Seq[Input], metric : Metric) extends Operator
{
    require(inputs.size == 2, "Number of inputs must be 2. " + inputs.size + " given.")

    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double] =
    {
        val set1 = inputs(0).evaluate(sourceInstance, targetInstance)
        val set2 = inputs(1).evaluate(sourceInstance, targetInstance)

        for (str1 <- set1; str2 <- set2) yield metric.evaluate(str1, str2)
    }

    override def toString = metric match
    {
        case Metric(name, params) => "Aggregation(weight=" + weight + ", type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
    }
}
