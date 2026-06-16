package org.silkframework.rule.plugins.distance.characterbased

import org.silkframework.rule.similarity.{BooleanDistanceMeasure, NonSymmetricDistanceMeasure, SingleValueDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin, PluginReference}

@Plugin(
  id = IsSubstringDistance.pluginId,
  categories = Array("Characterbased"),
  label = "Is substring",
  description = "Checks if a source value is a substring of a target value.",
  relatedPlugins = Array(
    new PluginReference(
      id = StartsWithDistance.pluginId,
      description = "The Starts With plugin tests a stricter condition: not only must the target appear in the source, but it must appear at the very start."
    ),
    new PluginReference(
      id = SubStringDistance.pluginId,
      description = "Containment and similarity are not the same measure. Is substring checks whether the source string appears anywhere inside the target and returns a binary result; Substring comparison scores the overall similarity between the two strings."
    )
  )
)
case class IsSubstringDistance(@Param("Reverse source and target inputs")
                               reverse: Boolean = false) extends SingleValueDistanceMeasure with NonSymmetricDistanceMeasure with BooleanDistanceMeasure {

  override def evaluate(value1: String, value2: String, limit: Double): Double = {
    if(value2.contains(value1)) {
      0.0
    } else {
      1.0
    }
  }
}

object IsSubstringDistance {
  final val pluginId = "isSubstring"
}
