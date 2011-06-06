package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class DateTimeMetricTest extends FlatSpec with ShouldMatchers
{
  val metric = new DateTimeMetric(60)
  val t = 0.9

  "DateTimeMetric" should "not return values under 0.0" in
  {
    metric.evaluate("2010-09-24T05:00:00", "2010-09-24T06:00:00") should be (approximatelyEqualTo (0.0))
  }

  "DateTimeMetric" should "return 1.0 if the dates are equal" in
  {
    metric.evaluate("2010-09-24T05:00:00", "2010-09-24T05:00:00") should be (approximatelyEqualTo (1.0))
  }

  "DateTimeMetric" should "return the correct similarity" in
  {
    metric.evaluate("2001-10-26T21:32:10", "2001-10-26T21:32:40") should be (approximatelyEqualTo (0.5))
  }
}