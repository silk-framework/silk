package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo

class NumMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new NumMetric()

    "NumMetric" should "return min((num1/num2), (num2/num1))" in
    {
        metric.evaluate("0.3", "0.7") should be (approximatelyEqualTo (0.428571))
        metric.evaluate("0.7", "0.3") should be (approximatelyEqualTo (0.428571))
        metric.evaluate("3000", "7000") should be (approximatelyEqualTo (0.428571))
        metric.evaluate("33", "45") should be (approximatelyEqualTo (0.73333333))
    }

    "NumMetric" should "return 1.0 if the numbers are equal" in
    {
        metric.evaluate("0", "0") should be (approximatelyEqualTo (1.0))
        metric.evaluate("123456", "123456") should be (approximatelyEqualTo (1.0))
        metric.evaluate("0.3", "0.3") should be (approximatelyEqualTo (1.0))
    }

    "NumMetric" should "return 0.0 if one number is 0" in
    {
        metric.evaluate("0", "1") should be (approximatelyEqualTo (0.0))
        metric.evaluate("1", "0") should be (approximatelyEqualTo (0.0))
    }
}
