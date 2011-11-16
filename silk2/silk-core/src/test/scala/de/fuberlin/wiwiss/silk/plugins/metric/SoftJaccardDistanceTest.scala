package de.fuberlin.wiwiss.silk.plugins.metric

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

@RunWith(classOf[JUnitRunner])
class SoftJaccardDistanceTest extends FlatSpec with ShouldMatchers {

  val distance = SoftJaccardDistance(maxDistance = 1)

  "SoftJaccardDistance" should "return soft jaccard distance" in {
    distance("AA" :: "BB" :: Nil, "CC" :: "DD" :: Nil) should be(approximatelyEqualTo(1.0))
    distance("AA" :: "BB" :: "C" :: Nil, "AA" :: "B" :: "CC" :: Nil) should be(approximatelyEqualTo(0.0))
    distance("Same1" :: "Different11" :: Nil, "Same2" :: "Different22" :: Nil) should be(approximatelyEqualTo(2.0 / 3.0))
    distance("A1" :: "B1" :: "CC" :: Nil, "A2" :: "B2" :: "DD" :: Nil) should be(approximatelyEqualTo(0.5))
  }

}