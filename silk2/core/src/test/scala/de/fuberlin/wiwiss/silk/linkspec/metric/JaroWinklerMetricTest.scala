package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.metric.JaroWinklerMetric
import org.scalatest.matchers.{BeMatcher, MatchResult, ShouldMatchers}
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo

class JaroWinklerMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new JaroWinklerMetric()

    "JaroWinklerMetric" should "return distance 0.832" in
    {
        metric.evaluateDistance("JONES", "JOHNSON") should be (approximatelyEqualTo (0.832))
        metric.evaluateDistance("JOHNSON", "JONES") should be (approximatelyEqualTo (0.832))
    }
}
