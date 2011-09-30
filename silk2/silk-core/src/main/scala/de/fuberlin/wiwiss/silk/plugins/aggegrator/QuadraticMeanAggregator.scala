package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.Aggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.Index

/**
 * Computes the weighted quadratic mean.
 */
@Plugin(id = "quadraticMean", label = "Euclidian distance", description = "Calculates the Euclidian distance.")
case class QuadraticMeanAggregator() extends Aggregator {
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (!values.isEmpty) {
      val sqDistance = values.map { case (weight, value) => weight * value * value }.reduceLeft(_ + _)
      val totalWeights = values.map { case (weight, value) => weight }.sum

      Some(math.sqrt(sqDistance / totalWeights))
    }
    else {
      None
    }
  }

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index) = index1 conjunction index2
}