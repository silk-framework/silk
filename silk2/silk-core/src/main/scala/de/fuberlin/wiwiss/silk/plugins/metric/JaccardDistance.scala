package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "jaccard", label = "Jaccard", description = "Jaccard similarity coefficient.")
class JaccardDistance extends DistanceMeasure {
  private val blockCount = 1000

  override def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size
    val unionSize = (set1 union set2).size

    1.0 - intersectionSize.toDouble / unionSize
  }

  override def index(values: Set[String], limit: Double): Set[Seq[Int]] = {
    //The number of values we need to index
    val indexSize = math.round(values.size * limit + 0.5).toInt

    values.take(indexSize).map(value => Seq((value.hashCode % blockCount).abs))
  }

  override def blockCounts(threshold: Double): Seq[Int] = {
    Seq(blockCount)
  }
}