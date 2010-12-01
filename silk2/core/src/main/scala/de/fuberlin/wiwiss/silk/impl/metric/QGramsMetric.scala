package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * String similarity based on q-grams.
 *
 * Parameters:
 * - '''q''' (optional): The size of the sliding window. Default: 2
 */
class QGramsMetric(val params : Map[String, String] = Map.empty) extends Metric
{
    private val q = readOptionalIntParam("q").getOrElse(2)

    override def evaluate(str1 : String, str2 : String, threshold : Double) =
    {
        val qGrams1 = str1.qGrams(q)
        val qGrams2 = str2.qGrams(q)

        val matchingQGrams = (qGrams1 intersect qGrams2).size * 2
        val numQGrams = qGrams1.size + qGrams2.size

        if (numQGrams == 0) 0.0
        else matchingQGrams.toDouble / numQGrams
    }
}
