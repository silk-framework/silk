package de.fuberlin.wiwiss.silk.evaluation

import math.abs

case class VariableStatistic(mean: Double, standardDeviation: Double) {
  def map(f: Double => Double) = {
    VariableStatistic(f(mean), f(standardDeviation))
  }
}

object VariableStatistic {
  def apply(values: Traversable[Double]): VariableStatistic = {
    val mean = values.sum / values.size
    val standardDeviation = values.map(x => abs(x - mean)).sum / values.size

    VariableStatistic(mean, standardDeviation)
  }
}