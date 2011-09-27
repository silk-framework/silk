package de.fuberlin.wiwiss.silk.plugins.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

class JaccardDistanceTest extends FlatSpec with ShouldMatchers {
  val distance = new JaccardDistance()

  "JaccardDistance" should "return jaccard coefficient" in {
    distance("A" :: "B" :: Nil, "C" :: "D" :: Nil) should be(approximatelyEqualTo(1.0))
    distance("Same" :: "Different1" :: Nil, "Same" :: "Different2" :: Nil) should be(approximatelyEqualTo(2.0 / 3.0))
    distance("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "D" :: Nil) should be(approximatelyEqualTo(0.5))
    distance("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "C" :: Nil) should be(approximatelyEqualTo(0.0))
  }

  "JaccardDistance" should "index one values if the limit is 0" in {
    distance.index(Set("A", "B", "C"), 0.0).size should equal (1)
  }

  "JaccardDistance" should "index all values if the limit is 1" in {
    distance.index(Set("A", "B", "C"), 1.0).size should equal (3)
  }

  "JaccardDistance" should "index the minimum number of values" in {
    distance.index(Set("A"), 0.5).size should equal (1)
    distance.index(Set("A", "B"), 0.5).size should equal (2)
    distance.index(Set("A", "B", "C"), 0.5).size should equal (2)
    distance.index(Set("A", "B", "C", "D"), 0.5).size should equal (3)
    distance.index(Set("A", "B", "C", "D", "E"), 0.5).size should equal (3)
    distance.index(Set("A", "B", "C", "D", "E", "F"), 0.5).size should equal (4)
    distance.index(Set("A"), 0.2).size should equal (1)
    distance.index(Set("A", "B"), 0.2).size should equal (1)
    distance.index(Set("A", "B", "C"), 0.2).size should equal (1)
    distance.index(Set("A", "B", "C", "D"), 0.2).size should equal (1)
    distance.index(Set("A", "B", "C", "D", "E"), 0.2).size should equal (2)
    distance.index(Set("A", "B", "C", "D", "E", "F"), 0.2).size should equal (2)
  }
}