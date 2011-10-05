package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(id = "softjaccard", label = "Soft Jaccard", description = "Soft Jaccard similarity coefficient.")
case class SoftJaccardDistance() extends DistanceMeasure {

  private val maxDistance = 0.1

  private val metric = LevenshteinMetric()

  override def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    val intersectionScore = values1.map(v1 => values2.map(v2 => metric.evaluate(v1, v2, maxDistance)).min).filter(_ > maxDistance).map(1.0 - _).sum
    val unionSize = (values1 ++ values2).toSet.size

    1.0 - intersectionScore / unionSize
  }

//  private def similarityScore(v1: String, values2: Traversable[String]) = {
//  }

//TODO indexing
//  override def index(values: Set[String], limit: Double) = {
//    //The number of values we need to index
//    val indexSize = math.round(values.size * limit + 0.5).toInt
//
//    Index.oneDim(values.take(indexSize).map(_.hashCode))
//  }
}