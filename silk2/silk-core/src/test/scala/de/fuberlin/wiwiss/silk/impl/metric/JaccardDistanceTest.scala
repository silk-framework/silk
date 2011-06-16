package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class JaccardDistanceTest extends FlatSpec with ShouldMatchers
{
  val distance = new JaccardDistance()

  "JaccardDistance" should "return jaccard coefficient" in
  {
    distance("A" :: "B" :: Nil, "C" :: "D" :: Nil) should be (approximatelyEqualTo (1.0))
    distance("Same" :: "Different1" :: Nil, "Same" :: "Different2" :: Nil) should be (approximatelyEqualTo (2.0 / 3.0))
    distance("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "D" :: Nil) should be (approximatelyEqualTo (0.5))
    distance("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "C" :: Nil) should be (approximatelyEqualTo (0.0))
  }
}