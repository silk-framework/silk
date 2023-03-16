package org.silkframework.rule.plugins.distance.equality

import org.silkframework.rule.similarity.{NonSymmetricDistanceMeasure, SimpleDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{DistanceMeasurePlugin, DistanceMeasureRange, Param, Plugin}

@Plugin(
  id = "greaterThan",
  categories = Array("Equality"),
  label = "Greater than",
  description = "Checks if the source value is greater than the target value. " +
  "If both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used."
)
@DistanceMeasurePlugin(
  range = DistanceMeasureRange.BOOLEAN,
)
case class GreaterThanMetric(@Param("Accept equal values")
                             orEqual: Boolean = false,
                             @Param("Per default, if both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used. Choose a more specific order for improved performance.")
                             order: OrderEnum = OrderEnum.autodetect,
                             @Param(value = "Reverse source and target inputs", advanced = true)
                             reverse: Boolean = false) extends SimpleDistanceMeasure with NonSymmetricDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    1.0 - LowerThanMetric.evaluate(str1, str2, !orEqual, order)
  }
}
