package de.fuberlin.wiwiss.silk.linkspec.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric

/**
 * String similarity based on q-grams.
 *
 * Parameters:
 * - '''q''' (optional): The size of the sliding window. Default: 2
 */
class QGramsMetric(val params : Map[String, String] = Map.empty) extends Metric
{
    private val q = readOptionalIntParam("q").getOrElse(2)

    override def evaluate(str1 : String, str2 : String) =
    {
        val boundary = "#" * (q - 1)

        val qGrams1 = (boundary + str1 + boundary).sliding(q).toSeq
        val qGrams2 = (boundary + str2 + boundary).sliding(q).toSeq

        val matchingQGrams = (qGrams1 intersect qGrams2).size * 2
        val numQGrams = qGrams1.size + qGrams2.size

        if (numQGrams == 0) 0.0
        else matchingQGrams.toDouble / numQGrams
    }
}
