package de.fuberlin.wiwiss.silk.plugins.distance.tokenbased

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin._
import de.fuberlin.wiwiss.silk.entity.Index

/**
 * Created by IntelliJ IDEA.
 * User: andreas
 * Date: 5/16/12
 * Time: 4:22 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Expects two vectors encoded as strings in this form: "term1 norm-score;term2 norm-score2..."
 */
@Plugin(
  id = "cosine",
  categories = Array("Tokenbased"),
  label = "Cosine",
  description = "Cosine Distance Measure."
)
case class CosineDistanceMetric(k: Int = 3) extends SimpleDistanceMeasure {
  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    val items1 = str1.split(";")
    val items2 = str2.split(";")
    val v1Map = items1.map(getValues(_)).toMap
    var similarity = 0.0
    for((entity, weight) <- items2.map(getValues(_))) {
      if(v1Map.contains(entity))
        similarity += weight*v1Map.get(entity).get
    }

    val distance = 1 - similarity
    if(distance < 0)
      0.0
    else distance
  }

  override def indexValue(str: String, limit: Double): Index = {
    if(str.trim()=="")
      return Index.empty
    val values = str.split(";")
    val list = values.map(getValues(_)).toSeq
    val topK = list.sortWith(_._2>_._2).take(k)
    Index.oneDim(topK.map(_.hashCode()).toSet)
  }

  private def getValues(item: String): (String, Double) = {
    val values = item.split(" ")
    return (values(0), values(1).toDouble)
  }
}
