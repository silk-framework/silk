package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance

trait Metric extends Operator
{
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double]

    val params : Map[String, Param]
}

object Metric
{
    def apply(aggType : String, weight : Int, params : Map[String, AnyParam]) : Metric =
    {
        throw new IllegalArgumentException("Metric type unknown: " + aggType)
    }
}
