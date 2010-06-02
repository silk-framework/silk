package de.fuberlin.wiwiss.silk.metric

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.linkspec.{AnyParam, Metric}

class LevenshteinMetric(val weight : Int, val params : Map[String, AnyParam]) extends Metric
{
    require(params.contains("str1"), "Parameter 'str1' is required")
    require(params.contains("str2"), "Parameter 'str2' is required")

    override def evaluate(instance1: Instance, instance2: Instance) =
    {
        val set1 = params("str1").evaluate(instance1, instance2)
        val set2 = params("str2").evaluate(instance1, instance2)
        for (str1 <- set1; str2 <- set2) yield
        {
            val levenshteinDistance = levenshtein(str1, str2)
            val maxDistance = Math.max(str1.length, str2.length)
            (1.0 - levenshteinDistance.toDouble / maxDistance.toDouble)
        }
    }

    private def levenshtein(str1 : String, str2 : String): Int =
    {
        val lenStr1 = str1.length
        val lenStr2 = str2.length

        val d: Array[Array[Int]] = new Array(lenStr1 + 1, lenStr2 + 1)

        for (val i <- 0 to lenStr1) d(i)(0) = i
        for (val j <- 0 to lenStr2) d(0)(j) = j

        for (val i <- 1 to lenStr1; val j <- 1 to lenStr2) {
            val cost = if (str1(i - 1) == str2(j - 1)) 0 else 1

            d(i)(j) = Math.min(
                d(i - 1)(j) + 1, // deletion
                Math.min(d(i)(j - 1) + 1, // insertion
                d(i - 1)(j - 1) + cost) // substitution
                )
        }
        return d(lenStr1)(lenStr2)
    }
}