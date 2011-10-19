package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

/**
 * String similarity based on q-grams.
 *
 * Parameters:
 * - '''q''' (optional): The size of the sliding window. Default: 2
 */
@Plugin(id = "qGrams", label = "qGrams", description = "String similarity based on q-grams (by default q=2).")
case class QGramsMetric(q: Int = 2) extends SimpleDistanceMeasure {
  private val diceCoefficient = DiceCoefficient()

  //TODO test with toSet?
  override def evaluate(str1: String, str2: String, threshold: Double) = {
    diceCoefficient(str1.qGrams(q), str2.qGrams(q), threshold)
  }

  override def indexValue(value: String, limit: Double): Index = {
    diceCoefficient.index(value.qGrams(q).toSet, limit)
  }
}
