package org.silkframework.rule.plugins.transformer.filter

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.util.MockServerTestTrait

class RemoveStopWordsTest extends AnyFlatSpec with Matchers with MockServerTestTrait {
  "RemoveStopWords" should "be case insensitive" in {
    val transformer = new RemoveStopWords()
    transformer.apply(Seq(Seq("To be or not to be", "that is the question"))).map(_.trim) should
      equal(Seq("", "question"))

    transformer.apply(Seq(Seq("It always seems impossible", "until it's done"))).map(_.trim) should
      equal(Seq("impossible", ""))
  }
}
