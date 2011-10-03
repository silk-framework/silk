package de.fuberlin.wiwiss.silk.evaluation.statistics


import math.abs

/**
 * Statistics about a stochastic variable such as the fMeasure of a set of linkage rules.
 */
case class VariableStatistic(mean: Double, standardDeviation: Double) {
  /**
   * Transforms this statistics by applying a function to all measures.
   */
  def map(f: Double => Double) = {
    VariableStatistic(f(mean), f(standardDeviation))
  }
}

/**
 * Generates the statistics for variable.
 */
object VariableStatistic {
  /**
   * Generates the statistics for variable based on a sample of values.
   */
  def apply(values: Traversable[Double]): VariableStatistic = {
    val mean = values.sum / values.size
    val standardDeviation = values.map(x => abs(x - mean)).sum / values.size

    VariableStatistic(mean, standardDeviation)
  }
}