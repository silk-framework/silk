package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.metric.JaroWinklerMetric
import org.scalatest.matchers.{BeMatcher, MatchResult, ShouldMatchers}

class JaroWinklerMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroWinklerMetric()

    "JaroWinklerMetric" should "return distance 0.832" in
    {
        metric.evaluateDistance("JONES", "JOHNSON") should be (approximatelyEqualTo (0.832))
        metric.evaluateDistance("JOHNSON", "JONES") should be (approximatelyEqualTo (0.832))
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
