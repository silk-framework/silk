package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import scala.math.max
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "levenshtein", label = "Normalized Levenshtein distance", description = "Normalized Levenshtein distance.")
case class LevenshteinMetric(minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {
  private val q = 1

  private val levenshtein = new LevenshteinDistance(minChar, maxChar)

  override def evaluate(str1: String, str2: String, limit: Double) = {
    val scale = max(str1.length, str2.length)

    levenshtein.evaluate(str1, str2, limit * scale) / scale
  }

  override def indexValue(str: String, limit: Double): Set[Seq[Int]] = {
    levenshtein.indexValue(str, limit * str.length)
  }

  override def blockCounts(threshold: Double): Seq[Int] = {
    levenshtein.blockCounts(threshold)
  }
}
