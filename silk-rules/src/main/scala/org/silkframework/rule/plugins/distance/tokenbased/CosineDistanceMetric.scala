package org.silkframework.rule.plugins.distance.tokenbased

import org.silkframework.entity.Index
import org.silkframework.rule.annotations.{DistanceMeasureExample, DistanceMeasureExamples}
import org.silkframework.rule.similarity.{NormalizedDistanceMeasure, SingleValueDistanceMeasure, TokenBasedDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.Plugin

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
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0 for identical unit vectors.",
    input1 = Array("a 1.0"),
    input2 = Array("a 1.0"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1 minus the dot product of the two vectors.",
    input1 = Array("a 0.8;b 0.6"),
    input2 = Array("a 0.5"),
    output = 0.6
  ),
  new DistanceMeasureExample(
    description = "Returns 1 for vectors that share no terms.",
    input1 = Array("a 1.0"),
    input2 = Array("b 1.0"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "Values that are not shaped as 'term score' do not match, instead of failing the linking execution.",
    input1 = Array("not a vector"),
    input2 = Array("a 1.0"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "Terms with a non-finite score are ignored.",
    input1 = Array("a NaN"),
    input2 = Array("a 1.0"),
    output = 1.0
  )
))
case class CosineDistanceMetric(k: Int = 3) extends SingleValueDistanceMeasure with TokenBasedDistanceMeasure with NormalizedDistanceMeasure {
  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    val items1 = str1.split(";")
    val items2 = str2.split(";")
    val v1Map = items1.flatMap(getValues).toMap
    var similarity = 0.0
    for((entity, weight) <- items2.flatMap(getValues)) {
      for(weight1 <- v1Map.get(entity)) {
        similarity += weight * weight1
      }
    }

    val distance = 1 - similarity
    if(distance < 0)
      0.0
    else distance
  }

  override def emptyIndex(limit: Double): Index = {
    Index.oneDim(Set.empty)
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    if(str.trim()=="")
      return Index.empty
    val values = str.split(";")
    val list = values.flatMap(getValues).toSeq
    val topK = list.sortWith(_._2>_._2).take(k)
    // Hash only the term: evaluate matches terms regardless of their weights, so the index must too.
    Index.oneDim(topK.map(_._1.hashCode).toSet)
  }

  // Items that are not shaped as "term score" or have a non-finite score are ignored instead of failing the whole linking run.
  private def getValues(item: String): Option[(String, Double)] = {
    val values = item.split(" ")
    if(values.length < 2) {
      None
    } else {
      values(1).toDoubleOption.filter(_.isFinite).map(score => (values(0), score))
    }
  }
}
