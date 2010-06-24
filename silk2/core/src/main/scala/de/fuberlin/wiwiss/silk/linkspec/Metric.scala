package de.fuberlin.wiwiss.silk.linkspec

import de.fuberlin.wiwiss.silk.metric.{JaroWinklerMetric, JaroDistanceMetric, LevenshteinMetric}
import de.fuberlin.wiwiss.silk.util.{Strategy, Factory}
import metric.{QGramsMetric, DateMetric, EqualityMetric, NumMetric}

trait Metric extends Strategy
{
    def evaluate(value1 : String, value2 : String) : Double
}

object Metric extends Factory[Metric]
{
    register("levenshtein", classOf[LevenshteinMetric])
    register("jaro", classOf[JaroDistanceMetric])
    register("jaroWinkler", classOf[JaroWinklerMetric])
    register("qGrams", classOf[QGramsMetric])
    register("equality", classOf[EqualityMetric])
    register("num", classOf[NumMetric])
    register("date", classOf[DateMetric])
}
