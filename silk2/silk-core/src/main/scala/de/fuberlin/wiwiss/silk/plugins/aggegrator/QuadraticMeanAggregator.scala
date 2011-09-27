package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkspec.similarity.MultiIndexAggregator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

/**
 * Computes the weighted quadratic mean.
 */
@Plugin(id = "quadraticMean", label = "Euclidian distance", description = "Calculates the Euclidian distance.")
class QuadraticMeanAggregator() extends MultiIndexAggregator {
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
}