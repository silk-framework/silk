package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

/**
 * String similarity based on q-grams.
 *
 * Parameters:
 * - '''q''' (optional): The size of the sliding window. Default: 2
 */
@Plugin(id = "qGrams", label = "qGrams", description = "String similarity based on q-grams (by default q=2).")
//TODO this is actually the Dice’s Coefficient with a qGrams tokenizer, if we add a qGrams tokenizer we could remove it...
case class QGramsMetric(q: Int = 2) extends SimpleDistanceMeasure {
  override def evaluate(str1: String, str2: String, threshold: Double) = {
    val qGrams1 = str1.qGrams(q)
    val qGrams2 = str2.qGrams(q)

    val matchingQGrams = (qGrams1.toSeq intersect qGrams2.toSeq).size * 2
    val numQGrams = qGrams1.size + qGrams2.size

    if (numQGrams == 0) 1.0
    else 1.0 - matchingQGrams.toDouble / numQGrams
  }
}
