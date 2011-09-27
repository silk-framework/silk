package de.fuberlin.wiwiss.silk.linkagerule.similarity

import math.min

/**
 * A simple similarity measure which compares pairs of values.
 */
abstract class SimpleDistanceMeasure extends DistanceMeasure {
  override final def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    var minDistance = Double.MaxValue

    for (str1 <- values1; str2 <- values2) {
      val distance = evaluate(str1, str2, min(limit, minDistance))
      minDistance = min(minDistance, distance)
    }

    minDistance
  }

  override final def index(values: Set[String], limit: Double): Set[Seq[Int]] =  {
    values.flatMap(value => indexValue(value, limit))
  }

  /**
   * Computes the similarity of a pair of values.
   */
  def evaluate(value1: String, value2: String, limit: Double = Double.PositiveInfinity): Double

  /**
   * Computes the index of a single value.
   */
  def indexValue(value: String, limit: Double): Set[Seq[Int]] = Set(Seq(0))
}