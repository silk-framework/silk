package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class DateTimeMetricTest extends FlatSpec with ShouldMatchers
{
  val metric = new DateTimeMetric()

  "DateTimeMetric" should "return 0.0 for equal values" in
  {
    metric.evaluate("2010-09-24T05:00:00", "2010-09-24T05:00:00") should be (approximatelyEqualTo (0.0))
  }

  "DateTimeMetric" should "return the correct similarity" in
  {
    metric.evaluate("2001-10-26T21:32:10", "2001-10-26T21:32:40") should be (approximatelyEqualTo (30.0))
  }
}