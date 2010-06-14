package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.{BeMatcher, MatchResult, ShouldMatchers}
import de.fuberlin.wiwiss.silk.metric.{JaroWinklerMetric, JaroDistanceMetric}
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo

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
}