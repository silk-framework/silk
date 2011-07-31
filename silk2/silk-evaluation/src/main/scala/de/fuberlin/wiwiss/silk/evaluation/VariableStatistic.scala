package de.fuberlin.wiwiss.silk.evaluation

import math.abs

case class VariableStatistic(mean: Double, standardDeviation: Double)

object VariableStatistic {
  def apply(values: Traversable[Double]): VariableStatistic = {
    val mean = values.sum / values.size
    val standardDeviation = values.map(x => abs(x - mean)).sum / values.size

    VariableStatistic(mean, standardDeviation)
  }
}