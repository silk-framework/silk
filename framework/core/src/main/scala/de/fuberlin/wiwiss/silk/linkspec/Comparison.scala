package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

class Comparison(val weight : Int, val inputs : Seq[Input], val metric : Metric) extends Operator
{
    require(inputs.size == 2, "Number of inputs must be 2. " + inputs.size + " given.")

    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double] =
    {
        val set1 = inputs(0).evaluate(sourceInstance, targetInstance)
        val set2 = inputs(1).evaluate(sourceInstance, targetInstance)

        for (str1 <- set1; str2 <- set2) yield metric.evaluate(str1, str2)
    }
}