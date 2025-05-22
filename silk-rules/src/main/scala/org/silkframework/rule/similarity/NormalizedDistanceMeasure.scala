package org.silkframework.rule.similarity

/**
  * Normalized distance measures always return values between 0 and 1.
  */
trait NormalizedDistanceMeasure extends DistanceMeasure {

  override def validateThreshold(threshold: Double): Option[String] = {
    super.validateThreshold(threshold) orElse {
      if (threshold > 1.0) {
        Some("For normalized distance measures, the threshold must be smaller or equal than 1.")
      } else {
        None
      }
    }
  }

  override def isNormalized: Boolean = true

}
