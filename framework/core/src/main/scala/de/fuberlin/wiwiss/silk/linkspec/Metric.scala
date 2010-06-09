package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.metric.{JaroWinklerMetric, JaroDistanceMetric, LevenshteinMetric}

trait Metric
{
    val params : Map[String, String]

    def evaluate(value1 : String, value2 : String) : Double
}

object Metric
{
    def apply(metricType : String, params : Map[String, String]) : Metric =
    {
        metricType match
        {
            case "levenshtein" => new LevenshteinMetric(params)
            case "jaro" => new JaroDistanceMetric(params)
            case "jaroWinkler" => new JaroWinklerMetric(params)
            case _ => throw new IllegalArgumentException("Metric type unknown: " + metricType)
        }
    }
}
