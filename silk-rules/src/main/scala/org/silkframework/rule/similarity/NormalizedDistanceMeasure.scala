package org.silkframework.rule.similarity

/**
  * This distance measure is not normalized, i.e., all distances start at 0 (exact match) and increase the more different the values are.
  */
trait NormalizedDistanceMeasure extends DistanceMeasure
