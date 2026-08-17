package org.silkframework.rule.plugins.distance.equality

import org.silkframework.rule.similarity.{BooleanDistanceMeasure, NonSymmetricDistanceMeasure, SingleValueDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}
import org.silkframework.runtime.plugin.types.CompareOrder

@Plugin(
  id = LowerThanMetric.pluginId,
  categories = Array("Equality"),
  label = "Lower than",
  description = "Checks if the source value is lower than the target value.",
  relatedPlugins = Array(
    new PluginReference(
      id = GreaterThanMetric.pluginId,
      description = "The greater than plugin tests the same pair of values with the ordering flipped: it succeeds where the lower than plugin fails."
    )
  )
)
case class LowerThanMetric(@Param("Accept equal values")
                           orEqual: Boolean = false,
                           @Param("Per default, if both strings are numbers, numerical order is used for comparison. Otherwise, alphanumerical order is used. Choose a more specific order for improved performance.")
                           order: CompareOrder = CompareOrder.autodetect,
                           @Param(value = "Reverse source and target inputs", advanced = true)
                           reverse: Boolean = false) extends SingleValueDistanceMeasure with NonSymmetricDistanceMeasure with BooleanDistanceMeasure {

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    if(order.isLower(str1, str2, orEqual)) 0.0 else 1.0
  }
}

object LowerThanMetric {
  final val pluginId = "lowerThan"
}
