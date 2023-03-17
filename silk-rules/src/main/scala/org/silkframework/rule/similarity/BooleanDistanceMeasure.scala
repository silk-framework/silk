package org.silkframework.rule.similarity

import org.silkframework.runtime.validation.ValidationException

/**
  * This is a boolean distance measure, i.e., all distances are either 0 or 1.
  */
trait BooleanDistanceMeasure { self: DistanceMeasure =>

  override def apply(values1: Seq[String], values2: Seq[String], limit: Double = Double.PositiveInfinity): Double = {
    val distance = self.apply(values2, values1, limit)
    if(distance == 0.0 || distance == 1.0) {
      distance
    } else {
      throw new ValidationException(s"Boolean distance measure ${this.getClass} returned an invalid value $distance.")
    }
  }

}
