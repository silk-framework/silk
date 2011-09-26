package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "equality", label = "Equality", description = "Return 1 if strings are equal, 0 otherwise.")
class EqualityMetric() extends SimpleDistanceMeasure {
  private val blockCount = 1000

  override def evaluate(str1: String, str2: String, threshold: Double) = if (str1 == str2) 0.0 else 1.0

  override def indexValue(str: String, threshold: Double): Set[Seq[Int]] = {
    Set(Seq((str.hashCode % blockCount).abs))
  }

  override def blockCounts(threshold: Double): Seq[Int] = {
    Seq(blockCount)
  }
}
