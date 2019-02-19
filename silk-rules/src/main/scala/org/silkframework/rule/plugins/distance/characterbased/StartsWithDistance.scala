package org.silkframework.rule.plugins.distance.characterbased

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.{NonSymmetricDistanceMeasure, SimpleDistanceMeasure}
import org.silkframework.runtime.plugin.{Param, Plugin}

@Plugin(
  id = "startsWith",
  categories = Array("Characterbased"),
  label = "Starts With",
  description = "Return 0 if the first string starts with the second string, 1 otherwise."
)
case class StartsWithDistance(@Param("Reverse source and target values")
                              reverse: Boolean = false,
                              @Param(label = "Min length", value = "The minimum length of the string being contained.")
                              minLength: Int = StartsWithDistance.DEFAULT_MIN_LENGTH,
                              @Param(label = "Max length", value = StartsWithDistance.MAX_LENGTH_DESCRIPTION)
                              maxLength: Int = StartsWithDistance.DEFAULT_MAX_LENGTH) extends SimpleDistanceMeasure with NonSymmetricDistanceMeasure {

  override def evaluate(value1: String, value2: String, limit: Double): Double = {
    val prefix = if(value2.length > maxLength) value2.take(maxLength) else value2
    if (value1.startsWith(prefix)) {
      0.0
    } else {
      1.0
    }
  }

  override def indexValue(value: String, limit: Double, sourceOrTarget: Boolean): Index = {
    val prefixStrings = if(sourceOrTarget) {
      var prefix = value.take(minLength - 1)
      for (ch <- value.drop(minLength - 1) if prefix.length < maxLength) yield {
        prefix += ch
        prefix
      }
    } else {
      if(value.length > maxLength) {
        Seq(value.take(maxLength))
      } else {
        Seq(value)
      }
    }
    Index.oneDim(prefixStrings.map(_.hashCode()).toSet)
  }
}

object StartsWithDistance {
  final val DEFAULT_MIN_LENGTH = 2
  final val DEFAULT_MAX_LENGTH = Int.MaxValue
  final val MAX_LENGTH_DESCRIPTION = "The potential maximum length of the strings that must match. If the max length is greater  " +
      "than the length of the string to match, the full string must match."
}