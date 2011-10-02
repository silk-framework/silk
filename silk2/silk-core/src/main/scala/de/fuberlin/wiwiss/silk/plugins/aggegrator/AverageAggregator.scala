package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.Aggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(id = "average", label = "Average", description = "Computes the weighted average.")
case class AverageAggregator() extends Aggregator {
  private val positiveWeight: Int = 9
  private val negativeWeight: Int = 10

  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (!values.isEmpty) {
      var sumWeights = 0
      var sumValues = 0.0

      for ((weight, value) <- values) {
        if (value >= 0.0) {
          sumWeights += weight * positiveWeight
          sumValues += weight * positiveWeight * value
        }
        else if (value < 0.0) {
          sumWeights += weight * negativeWeight
          sumValues += weight * negativeWeight * value
        }
      }

      val average = sumValues / sumWeights

      Some(average)
    }
    else {
      None
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 conjunction index2

  override def computeThreshold(limit: Double, weight: Double): Double = {
    1.0 - ((1.0 - limit) / weight) + positiveWeight.toDouble / negativeWeight
  }
}
