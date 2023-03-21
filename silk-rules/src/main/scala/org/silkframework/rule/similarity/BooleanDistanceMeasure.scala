package org.silkframework.rule.similarity

/**
  * This is a boolean distance measure, i.e., all distances are either 0 or 1.
  */
trait BooleanDistanceMeasure extends DistanceMeasure {

  override def validateThreshold(threshold: Double): Option[String] = {
    super.validateThreshold(threshold) orElse {
      if (threshold > 0.0) {
        Some("For boolean distance measures, the threshold is ignored and should not be changed from 0.")
      } else {
        None
      }
    }
  }

}
