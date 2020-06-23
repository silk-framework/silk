package org.silkframework.rule.plugins.distance.equality

import org.silkframework.test.PluginTest

class NumericEqualityMetricTest extends PluginTest {
  behavior of "Numeric Equality Metric"

  it should "match numbers with exact precision" in {
    implicit val numericEquality: NumericEqualityMetric = NumericEqualityMetric()
    matches("0.000000001", "1.0e-9")
    matches("1", "1.000000000")
    matches("0.0", "0.0E0")
    matches("0.0", "0")
    matches("1.23E2", "123")
    matchesNot("1E7", "1E8")
    matchesNot("1", "1.0000000001")
    matchesNot("123.123", "123.1230000001")
  }

  it should "match numbers with small tolerance in precision" in {
    implicit val numericEquality: NumericEqualityMetric = NumericEqualityMetric(precision = 0.001)
    matches("0.000000001", "1.0e-9")
    matches("1", "1.000000000")
    matches("0.0", "0.0E0")
    matches("0.0", "0")
    matches("1.23E2", "123")
    matches("1", "1.0000000001")
    matches("123.123", "123.1230000001")
    matches("1.000999999", "1")
    matches("0.999000001", "1")
    matches("1", "1.000999")
    matches("1", "0.99900000001")
    matchesNot("1.00100001", "1")
    matchesNot("0.99899999", "1")
    matchesNot("1", "1.00100001")
    matchesNot("1", "0.99899999")
  }

  it should "not allow invalid precision values" in {
    intercept[IllegalArgumentException] {
      NumericEqualityMetric(precision = 1.0)
    }
    intercept[IllegalArgumentException] {
      NumericEqualityMetric(precision = -0.1)
    }
  }

  it should "have matching index values for values that should match" in {
    indexMatches(1.0, 1.0, 0.0)
    indexMatches(0.01, 0.01, 0.0)
    indexMatches(1000, 1000, precision = 0.0)
    indexMatches(23.000, 23.000999999, precision = 0.001)
    indexMatches(23.000, 22.999000001, precision = 0.001)
    indexMatches(2.1235273, 2.123999001, precision = 0.001)
    indexMatches(2.1235273, 2.123999001, precision = 0.001)
    indexMatches(1.0, 0.9999, precision = 0.001)
    indexShouldNotMatch(1.0, 1.0000001, precision = 0.0)
    indexShouldNotMatch(23.000, 23.0012, precision = 0.001)
    indexShouldNotMatch(1.12345, 1.12345 + 0.0045, precision = 0.0015)
    indexShouldNotMatch(1.12345, 1.12345 - 0.0045, precision = 0.0015)
    for(i <- 0.0 until 0.0015 by 0.000001) {
      val baseNumber = 23.424242
      val precision = 0.0015
      indexMatches(baseNumber, baseNumber + i, precision)
      indexMatches(baseNumber, baseNumber - i, precision)
      // Precision of 3 is needed because both numbers are normalized, rounded and precision added/removed, so else they could overlap
      indexShouldNotMatch(baseNumber, baseNumber + (precision * 3 + i), precision)
      indexShouldNotMatch(baseNumber, baseNumber - (precision * 3 + i), precision)
    }
  }

  private def indexMatches(doubleValue1: Double, doubleValue2: Double, precision: Double): Unit = {
    val comparison = NumericEqualityMetric(precision)
    val index1 = comparison.indexValue(doubleValue1.toString, 0.0, sourceOrTarget = true).flatten
    val index2 = comparison.indexValue(doubleValue2.toString, 0.0, sourceOrTarget = true).flatten
    assert((index1 intersect index2).nonEmpty,
      s"Double values should have had matching indexes with precision $precision: ($doubleValue1, $doubleValue2)")
  }

  private def indexShouldNotMatch(doubleValue1: Double, doubleValue2: Double, precision: Double): Unit = {
    val comparison = NumericEqualityMetric(precision)
    val index1 = comparison.indexValue(doubleValue1.toString, 0.0, sourceOrTarget = true).flatten
    val index2 = comparison.indexValue(doubleValue2.toString, 0.0, sourceOrTarget = true).flatten
    assert((index1 intersect index2).isEmpty,
      s"Double values should not have matching indexes with precision $precision, but do have: ($doubleValue1, $doubleValue2)")
  }

  private def matches(numericString1: String,
                      numericString2: String)
                     (implicit numericEquality: NumericEqualityMetric): Unit = {
    assert(
      numericEquality.evaluate(numericString1, numericString2) == 0.0,
      s"Numeric values did not match: ($numericString1, $numericString2)"
    )
  }

  private def matchesNot(numericString1: String,
                      numericString2: String)
                     (implicit numericEquality: NumericEqualityMetric): Unit = {
    assert(
      numericEquality.evaluate(numericString1, numericString2) == 1.0,
      s"Numeric values matched, but should not match: ($numericString1, $numericString2)"
    )
  }

  override protected def pluginObject: AnyRef = NumericEqualityMetric()
}
