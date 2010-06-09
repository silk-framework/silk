package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.{BeMatcher, MatchResult, ShouldMatchers}
import de.fuberlin.wiwiss.silk.metric.{JaroWinklerMetric, JaroDistanceMetric}

class JaroDistanceMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroDistanceMetric()

    "JaroDistanceMetric" should "return distance 0.767" in
    {
        metric.evaluate("DIXON", "DICKSONX") should be (approximatelyEqualTo (0.767))
        metric.evaluate("DICKSONX", "DIXON") should be (approximatelyEqualTo (0.767))
    }

    "JaroDistanceMetric" should "return distance 0.944" in
    {
        metric.evaluate("MARTHA", "MARHTA") should be (approximatelyEqualTo (0.944))
        metric.evaluate("MARHTA", "MARTHA") should be (approximatelyEqualTo (0.944))
    }

    /**
     * Matcher to test if 2 values are approximately equal.
     */
    case class approximatelyEqualTo(r : Double) extends BeMatcher[Double]
    {
        val epsilon = 0.001

        def apply(l: Double) =
            MatchResult(
                compare(l, r),
                l + " is not approximately equal to " + r,
                l + " is approximately equal to " + r
            )

        private def compare(l : Double, r : Double) : Boolean =
        {
            math.abs(l - r) < epsilon
        }
    }
}