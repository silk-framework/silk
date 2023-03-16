package org.silkframework.rule.plugins.distance.characterbased

import org.silkframework.rule.similarity.{NonSymmetricDistanceMeasure, SimpleDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{DistanceMeasurePlugin, DistanceMeasureRange, Param, Plugin}

@Plugin(
  id = "isSubstring",
  categories = Array("Characterbased"),
  label = "Is substring",
  description = "Checks if a source value is a substring of a target value."
)
@DistanceMeasurePlugin(
  range = DistanceMeasureRange.BOOLEAN
)
case class IsSubstringDistance(@Param("Reverse source and target inputs")
                               reverse: Boolean = false) extends SimpleDistanceMeasure with NonSymmetricDistanceMeasure {

  override def evaluate(value1: String, value2: String, limit: Double): Double = {
    if(value2.contains(value1)) {
      0.0
    } else {
      1.0
    }
  }
}
