package org.silkframework.rule.plugins.distance.equality

import java.math.RoundingMode
import java.text.DecimalFormat

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.{Param, Plugin}

@Plugin(
  id = "numericEquality",
  categories = Array("Equality", "Recommended"),
  label = "Numeric Equality",
  description = "Compares values numerically instead of their string representation as the 'String Equality' operator does. " +
      "Allows to set the needed precision of the comparison. A value of 0.0 means that the values must represent exactly the same" +
      " (floating point) value, values higher than that allow for a margin of tolerance. Example: With a precision of 0.1, the" +
      " following pairs of values will be considered equal: (1.3, 1.35), (0.0, 0.9999), (0.0, -0.90001), but following pairs will NOT match:" +
      " (1.2, 1.30001), (1.0, 1.10001), (1.0, 0.89999)."
)
case class NumericEqualityMetric(@Param("The range of tolerance in floating point number comparisons. Must be 0 or a non-negative number smaller than 1.")
                                 precision: Double = 0.0) extends SimpleDistanceMeasure {
  val MAX_SIGNIFICANT_DECIMAL_PLACE = 50

  if(precision >= 1.0 || precision < 0.0) {
    throw new IllegalArgumentException("precision parameter must be 0 or a non-negative number smaller than 1.")
  }

  /** The decimal place that can definitely lead to different values when changing it. */
  val significantDecimalPlace: Int = {
    val decimalPlace = math.ceil(math.abs(math.log10(precision))).toInt
    if(decimalPlace > MAX_SIGNIFICANT_DECIMAL_PLACE) {
      MAX_SIGNIFICANT_DECIMAL_PLACE
    } else {
      decimalPlace
    }
  }

  // A double formatter that formats the number in a way that it can be indexed
  private val indexFormat = {
    val formatter = new DecimalFormat("#." + ("#" * significantDecimalPlace))
    formatter.setRoundingMode(RoundingMode.DOWN)
    formatter
  }

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    val d1 = str1.toDouble
    val d2 = str2.toDouble
    if (precision == 0.0) {
      if (d1 == d2) 0.0 else 1.0
    } else {
      if (math.abs(d1 - d2) <= precision) 0.0 else 1.0
    }
  }

  private def hashFormattedDouble(double: Double): Int = {
    val formatted = indexFormat.format(double)
    formatted.hashCode
  }

  override def indexValue(str: String, threshold: Double, sourceOrTarget: Boolean): Index = {
    val doubleValue = str.toDouble
    val indexValues = if(precision == 0.0) {
      Set(doubleValue.hashCode())
    } else {
      val normalizedDoubleValue = doubleValue - (doubleValue % precision)
      val oneLower = normalizedDoubleValue - 1.1 * precision
      val oneHigher = normalizedDoubleValue + 1.1 * precision
      Set(doubleValue, oneLower, oneHigher).map(hashFormattedDouble)
    }
    Index.oneDim(indexValues)
  }
}
