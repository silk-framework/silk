package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo
import org.scalatest.matchers.ShouldMatchers

class QGramsMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new QGramsMetric()

    "QGramsMetric" should "return 0.0 if the input strings do not share a single q-gram" in
    {
        metric.evaluate("abcd", "dcba") should be (approximatelyEqualTo (0.0))
    }

    "QGramsMetric" should "return 1.0 if the input strings are equal" in
    {
        metric.evaluate("abcd", "abcd") should be (approximatelyEqualTo (1.0))
    }

    "QGramsMetric" should "return matchingQGrams / numQGrams" in
    {
        //q-grams = (#a, ab, b#) and (#a, ac, c#), matchingQGrams = 1, numQGrams = 3
        metric.evaluate("ab", "ac") should be (approximatelyEqualTo (1.0 / 3.0))
    }
}
