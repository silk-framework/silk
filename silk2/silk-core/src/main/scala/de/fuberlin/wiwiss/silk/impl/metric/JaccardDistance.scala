package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation

@StrategyAnnotation(id = "jaccard", label = "Jaccard", description = "Jaccard similarity coefficient.")
class JaccardDistance extends DistanceMeasure {
  private val blockCount = 1000

  override def apply(values1: Traversable[String], values2: Traversable[String], threshold: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size
    val unionSize = (set1 union set2).size

    1.0 - intersectionSize.toDouble / unionSize
  }

//  override def index(str: String, threshold: Double): Set[Seq[Int]] = {
//    val
//    Set(Seq((str.hashCode % blockCount).abs))
//  }
//
//  override def blockCounts(threshold: Double): Seq[Int] = {
//    Seq(blockCount)
//  }
}