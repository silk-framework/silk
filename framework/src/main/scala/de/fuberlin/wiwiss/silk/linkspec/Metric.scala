package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.metric.LevenshteinMetric

trait Metric extends Operator
{
    def evaluate(sourceInstance : Instance, targetInstance : Instance) : Traversable[Double]

    val params : Map[String, AnyParam]
}

object Metric
{
    def apply(aggType : String, weight : Int, params : Map[String, AnyParam]) : Metric =
    {
        //TODO add missing metrics
        new LevenshteinMetric(weight, params)
        //throw new IllegalArgumentException("Metric type unknown: " + aggType)
    }
}
