package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(id = "dice", label = "Dice coefficient", description = "Dice similarity coefficient.")
case class DiceCoefficient extends DistanceMeasure {
  override def apply(values1: Traversable[String], values2: Traversable[String], threshold: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size * 2
    val totalSize = set1.size + set2.size

    1.0 - intersectionSize.toDouble / totalSize
  }

  override def index(values: Set[String], limit: Double): Index = {
    //The number of values we need to index
    val indexSize = math.round((2.0 * values.size * limit / (1 + limit)) + 0.5).toInt

    Index.oneDim(values.take(indexSize).map(value => value.hashCode))
  }
}