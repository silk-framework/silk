package org.silkframework.rule.plugins.distance.equality

import java.math.RoundingMode
import java.text.DecimalFormat
import org.silkframework.entity.Index
import org.silkframework.rule.similarity.{BooleanDistanceMeasure, SingleValueDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{DistanceMeasureExample, DistanceMeasureExamples, Param, Plugin}
import org.silkframework.runtime.plugin.PluginCategories

@Plugin(
  id = "numericEquality",
  categories = Array("Equality", PluginCategories.recommended),
  label = "Numeric equality",
  description = NumericEqualityMetric.description
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0 for equal numbers.",
    input1 = Array("4.2"),
    input2 = Array("4.2"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1 if at least one value is not a number.",
    input1 = Array("1"),
    input2 = Array("one"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "Returns 0 for numbers within the configured precision.",
    parameters = Array("precision", "0.1"),
    input1 = Array("1.3"),
    input2 = Array("1.35"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1 for numbers outside the configured precision.",
    parameters = Array("precision", "0.1"),
    input1 = Array("1.3"),
    input2 = Array("1.5"),
    output = 1.0
  )
))
case class NumericEqualityMetric(@Param("The range of tolerance in floating point number comparisons. Must be 0 or a non-negative number smaller than 1.")
                                 precision: Double = 0.0) extends SingleValueDistanceMeasure with BooleanDistanceMeasure {

  val MAX_SIGNIFICANT_DECIMAL_PLACE = 50

  if(precision >= 1.0 || precision < 0.0) {
    throw new IllegalArgumentException("precision parameter must be 0 or a non-negative number smaller than 1.")
  }

  /** The decimal place that can definitely lead to different values when changing it. */
  @transient
  val significantDecimalPlace: Int = {
    val decimalPlace = math.ceil(math.abs(math.log10(precision))).toInt
    if(decimalPlace > MAX_SIGNIFICANT_DECIMAL_PLACE) {
      MAX_SIGNIFICANT_DECIMAL_PLACE
    } else {
      decimalPlace
    }
  }

  // A double formatter that formats the number in a way that it can be indexed
  @transient
  private val indexFormat = new ThreadLocal[DecimalFormat]{
    override def initialValue(): DecimalFormat = {
      val formatter = new DecimalFormat("#." + ("#" * significantDecimalPlace))
      formatter.setRoundingMode(RoundingMode.DOWN)
      formatter
    }
  }

  override def evaluate(str1: String, str2: String, threshold: Double): Double = {
    try {
      val d1 = str1.toDouble
      val d2 = str2.toDouble
      if (precision == 0.0) {
        if (d1 == d2) 0.0 else 1.0
      } else {
        if (math.abs(d1 - d2) <= precision) 0.0 else 1.0
      }
    } catch {
      case _: NumberFormatException =>
        1.0
    }
  }

  private def hashFormattedDouble(double: Double): Int = {
    val formatted = indexFormat.get().format(double)
    formatted.hashCode
  }

  override def emptyIndex(limit: Double): Index = {
    Index.oneDim(Set.empty)
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    try {
      val doubleValue = str.toDouble
      val indexValues = if (precision == 0.0) {
        Set(doubleValue.hashCode())
      } else {
        val normalizedDoubleValue = doubleValue - (doubleValue % precision)
        val oneLower = normalizedDoubleValue - 1.1 * precision
        val oneHigher = normalizedDoubleValue + 1.1 * precision
        Set(doubleValue, oneLower, oneHigher).map(hashFormattedDouble)
      }
      Index.oneDim(indexValues)
    } catch {
      case _: NumberFormatException =>
        emptyIndex(limit)
    }
  }
}

object NumericEqualityMetric {
  final val description = """Compares values numerically instead of their string representation as the 'String Equality' operator does.
Allows to set the needed precision of the comparison. A value of 0.0 means that the values must represent exactly the same
(floating point) value, values higher than that allow for a margin of tolerance."""
}