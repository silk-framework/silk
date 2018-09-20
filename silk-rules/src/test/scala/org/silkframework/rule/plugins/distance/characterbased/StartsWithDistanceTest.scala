package org.silkframework.rule.plugins.distance.characterbased

import org.scalatest.{FlatSpec, MustMatchers}

class StartsWithDistanceTest extends FlatSpec with MustMatchers {
  behavior of "starts with distance"

  val distance = StartsWithDistance(minLength = 2)
  val EPS = 0.00001

  val prefix = "http://somePrefix/"
  val emptyPrefix = ""
  val nonMatchingUrls = Seq("http://someOtherPrefix/path", "urn:test:12", "", "http://somePrefix", "http", " http://somePrefix/")
  val matchingUrls = Seq(s"$prefix", s"${prefix}path", s"$prefix$prefix", s"$prefix ")

  it should "index the target values only as a whole and the source values as prefixes" in {
    val sourceIndex = distance.indexValue(prefix, 1.0, sourceOrTarget = true).flatten
    val targetIndex = distance.indexValue(prefix, 1.0, sourceOrTarget = false).flatten
    targetIndex.size mustBe 1
    sourceIndex.size mustBe (prefix.length - 1)
    sourceIndex.contains(targetIndex.head)
    val a1 = distance.indexValue("http://somePrefix/43/43", 1.0, sourceOrTarget = true)
    val a2 = distance.indexValue("http://somePrefix/107/107", 1.0, sourceOrTarget = false)
    a2
  }

  it should "match the correct pairs" in {
    for(nonMatch <- nonMatchingUrls) {
      distance.evaluate(nonMatch, prefix) mustBe 1.0 +- EPS
    }
    for(matching <- matchingUrls) {
      distance.evaluate(matching, prefix) mustBe 0.0 +- EPS
    }
  }

  it should "respect the max prefix length parameter" in {
    val d = StartsWithDistance(minLength = 2, maxLength = 5)
    for(matching <- matchingUrls) {
      d.evaluate(matching, prefix) mustBe 0.0 +- EPS
    }
    d.evaluate(prefix.take(5) + "X", prefix) mustBe 0.0 +- EPS
    d.evaluate(prefix.take(4) + "X", prefix) mustBe 1.0 +- EPS
    d.indexValue(prefix.take(5), 1.0, sourceOrTarget = false) mustBe d.indexValue(prefix.take(6), 1.0, sourceOrTarget = false)
    d.indexValue(prefix, 1.0, sourceOrTarget = true).flatten.size mustBe 4
  }
}
