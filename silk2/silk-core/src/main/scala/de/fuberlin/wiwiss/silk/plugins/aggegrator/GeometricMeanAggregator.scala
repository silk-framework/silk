package de.fuberlin.wiwiss.silk.plugins.aggegrator

import de.fuberlin.wiwiss.silk.linkagerule.similarity.Aggregator
import scala.math._
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

/**
 * Computes the weighted geometric mean.
 */
@Plugin(
  id = "geometricMean",
  label = "Geometric mean",
  description = "Compute the (weighted) geometric mean.")
case class GeometricMeanAggregator() extends Aggregator {
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

  /**
   * Combines two indexes into one.
   */
  override def combineIndexes(index1: Index, index2: Index)= index1 conjunction index2
}