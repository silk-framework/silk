package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index
import math.{min, max}

/**
 * String similarity based on q-grams.
 *
 * Parameters:
 * - '''q''' (optional): The size of the sliding window. Default: 2
 */
@Plugin(id = "qGrams", label = "qGrams", description = "String similarity based on q-grams (by default q=2).")
case class QGramsMetric(q: Int = 2, minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {
  private val jaccardCoefficient = JaccardDistance()

  //TODO test with toSet?
  override def evaluate(str1: String, str2: String, threshold: Double) = {
    jaccardCoefficient(str1.qGrams(q), str2.qGrams(q), threshold)
  }

  override def indexValue(value: String, limit: Double): Index = {
    val qGrams = value.qGrams(q)

    //The number of values we need to index
    val indexSize = math.round(qGrams.size * limit + 0.5).toInt

    val indices = qGrams.take(indexSize).map(indexQGram).toSet

    Index.oneDim(indices, BigInt(maxChar - minChar + 1).pow(q).toInt)
  }

  private def indexQGram(qGram: String): Int = {
    def combine(index: Int, char: Char) = {
      val croppedChar = min(max(char, minChar), maxChar)
      index * (maxChar - minChar + 1) + croppedChar - minChar
    }

    qGram.foldLeft(0)(combine)
  }
}
