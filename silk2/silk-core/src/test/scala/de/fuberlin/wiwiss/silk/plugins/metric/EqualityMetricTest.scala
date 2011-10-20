package de.fuberlin.wiwiss.silk.plugins.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EqualityMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new EqualityMetric()
  val t = 1.0

  "EqualityMetric" should "return 1.0 if the string differ" in {
    metric.evaluate("aaa", "aab", t) should be(approximatelyEqualTo(1.0))
    metric.evaluate("123", "124", t) should be(approximatelyEqualTo(1.0))
    metric.evaluate("", "xxx", t) should be(approximatelyEqualTo(1.0))
  }

  "EqualityMetric" should "return 0.0 if the string are equal" in {
    metric.evaluate("aaa", "aaa", t) should be(approximatelyEqualTo(0.0))
    metric.evaluate("  _", "  _", t) should be(approximatelyEqualTo(0.0))
    metric.evaluate("123", "123", t) should be(approximatelyEqualTo(0.0))
  }
}