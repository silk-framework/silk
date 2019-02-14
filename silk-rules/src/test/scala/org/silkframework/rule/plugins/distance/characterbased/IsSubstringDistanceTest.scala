package org.silkframework.rule.plugins.distance.characterbased

import org.scalatest.{FlatSpec, MustMatchers}

class IsSubstringDistanceTest extends FlatSpec with MustMatchers {

  behavior of "IsSubstringDistance"

  val distance = IsSubstringDistance()
  val distanceReversed = IsSubstringDistance(reverse = true)

  it should "match if a source value is a substring of a target value" in {
    distance(Seq("able"), Seq("Cables")) mustBe 0.0
    distance(Seq("Cables"), Seq("able")) mustBe 1.0
    distance(Seq("xxx", "able", "yyy"), Seq("House", "Bed", "Cables")) mustBe 0.0
  }

  it should "work with reversed inputs" in {
    distanceReversed(Seq("able"), Seq("Cables")) mustBe 1.0
    distanceReversed(Seq("Cables"), Seq("able")) mustBe 0.0
  }
}
