package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.MultiIndexAggregator
import scala.math._
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

/**
 * Computes the weighted geometric mean.
 */
@Plugin(
  id = "geometricMean",
  label = "Geometric mean",
  description = "Compute the (weighted) geometric mean.")
class GeometricMeanAggregator() extends MultiIndexAggregator {
  override def evaluate(values: Traversable[(Int, Double)]) = {
    if (!values.isEmpty) {
      val weightedProduct = values.map { case (weight, value) => pow(value, weight) }.reduceLeft(_ * _)
      val totalWeights = values.map { case (weight, value) => weight }.sum

      Some(pow(weightedProduct, 1.0 / totalWeights))
    }
    else {
      None
    }
  }
}