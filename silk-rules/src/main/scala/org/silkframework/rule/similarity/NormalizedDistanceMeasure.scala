package org.silkframework.rule.similarity

/**
  * This distance measure is not normalized, i.e., all distances start at 0 (exact match) and increase the more different the values are.
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

}
