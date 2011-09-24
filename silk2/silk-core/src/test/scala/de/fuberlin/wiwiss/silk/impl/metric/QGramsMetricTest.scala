package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class QGramsMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new QGramsMetric()
  val t = 0.9

  "QGramsMetric" should "return 1.0 if the input strings do not share a single q-gram" in {
    metric.evaluate("abcd", "dcba", t) should be(approximatelyEqualTo(1.0))
  }

  "QGramsMetric" should "return 0.0 if the input strings are equal" in {
    metric.evaluate("abcd", "abcd", t) should be(approximatelyEqualTo(0.0))
  }

  "QGramsMetric" should "return matchingQGrams / numQGrams" in {
    //q-grams = (#a, ab, b#) and (#a, ac, c#), matchingQGrams = 1, numQGrams = 3
    metric.evaluate("ab", "ac", t) should be(approximatelyEqualTo(2.0 / 3.0))
  }
}