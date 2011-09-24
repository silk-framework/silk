package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure

@StrategyAnnotation(id = "dice", label = "Dice coefficient", description = "Dice similarity coefficient.")
class DiceCoefficient extends DistanceMeasure {
  private val blockCount = 1000

  override def apply(values1: Traversable[String], values2: Traversable[String], threshold: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size * 2
    val totalSize = set1.size + set2.size

    1.0 - intersectionSize.toDouble / totalSize
  }

  override def index(values: Set[String], limit: Double): Set[Seq[Int]] = {
    //The number of values we need to index
    val indexSize = math.round((2.0 * values.size * limit / (1 + limit)) + 0.5).toInt

    values.take(indexSize).map(value => Seq((value.hashCode % blockCount).abs))
  }

  override def blockCounts(threshold: Double): Seq[Int] = {
    Seq(blockCount)
  }
}