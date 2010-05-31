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
        if(aggType == "levenshtein") new LevenshteinMetric(weight, params)
        else if(aggType == "jaroSimilarity") new LevenshteinMetric(weight, params) //TODO
        else
        {
            //Return dummy metric until all metrics are available
            new Metric
            {
                val weight = 0
                val params = Map[String, AnyParam]()
                def evaluate(sourceInstance : Instance, targetInstance : Instance) = Traversable()
            }
        }

        //throw new IllegalArgumentException("Metric type unknown: " + aggType)
    }
}
