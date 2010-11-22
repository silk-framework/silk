package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class EqualityMetricTest extends FlatSpec with ShouldMatchers
{
  val metric = new EqualityMetric()

  "EqualityMetric" should "return 0.0 if the string differ" in
  {
    metric.evaluate("aaa", "aab") should be (approximatelyEqualTo (0.0))
    metric.evaluate("123", "124") should be (approximatelyEqualTo (0.0))
    metric.evaluate("", "xxx") should be (approximatelyEqualTo (0.0))
  }

  "EqualityMetric" should "return 1.0 if the string are equal" in
  {
    metric.evaluate("aaa", "aaa") should be (approximatelyEqualTo (1.0))
    metric.evaluate("  _", "  _") should be (approximatelyEqualTo (1.0))
    metric.evaluate("123", "123") should be (approximatelyEqualTo (1.0))
  }
}