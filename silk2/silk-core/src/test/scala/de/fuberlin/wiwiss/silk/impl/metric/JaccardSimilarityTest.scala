package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.impl.util.approximatelyEqualTo

class JaccardSimilarityTest extends FlatSpec with ShouldMatchers
{
  val similarity = new JaccardSimilarity()

  "JaccardSimilarity" should "return jaccard coefficient" in
  {
    similarity("A" :: "B" :: Nil, "C" :: "D" :: Nil) should be (approximatelyEqualTo (0.0))
    similarity("Same" :: "Different1" :: Nil, "Same" :: "Different2" :: Nil) should be (approximatelyEqualTo (0.3333333))
    similarity("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "D" :: Nil) should be (approximatelyEqualTo (0.5))
    similarity("A" :: "B" :: "C" :: Nil, "A" :: "B" :: "C" :: Nil) should be (approximatelyEqualTo (1.0))
  }
}