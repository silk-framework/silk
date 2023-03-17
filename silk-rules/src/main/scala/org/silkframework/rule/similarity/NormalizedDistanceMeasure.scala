package org.silkframework.rule.similarity

import org.silkframework.runtime.validation.ValidationException

/**
  * This distance measure is not normalized, i.e., all distances start at 0 (exact match) and increase the more different the values are.
  */
trait NormalizedDistanceMeasure { self: DistanceMeasure =>

  override def apply(values1: Seq[String], values2: Seq[String], limit: Double = Double.PositiveInfinity): Double = {
    val distance = self.apply(values2, values1, limit)
    if(distance >= 0.0 || distance <= 1.0) {
      distance
    } else {
      throw new ValidationException(s"Normalized distance measure ${this.getClass} returned an invalid value $distance.")
    }
  }

}
