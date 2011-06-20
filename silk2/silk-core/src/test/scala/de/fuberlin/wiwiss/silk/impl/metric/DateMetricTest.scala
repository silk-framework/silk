package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class DateMetricTest extends FlatSpec with ShouldMatchers
{
  val metric = new DateMetric()
  val t = 0.9

  "DateMetric" should "not return values under 0.0" in
  {
    metric.evaluate("2003-03-01", "2010-09-30", 0.9) should be (approximatelyEqualTo (0.0))
  }

  "DateMetric" should "return 1.0 if the dates are equal" in
  {
    metric.evaluate("2010-09-30", "2010-09-30", 0.9) should be (approximatelyEqualTo (1.0))
  }

  "DateMetric" should "ignore the time of day" in
  {
    metric.evaluate("2010-09-24", "2010-09-30", 0.9) should be (approximatelyEqualTo (0.4))
    metric.evaluate("2010-09-24T06:00:00", "2010-09-30T06:00:00", 0.9) should be (approximatelyEqualTo (0.4))
  }
}
