package org.silkframework.plugins.transformer.conditional

import org.scalatest.{MustMatchers, FlatSpec}

/**
  * Created on 9/9/16.
  */
class IfMatchesRegexTransformerTest extends FlatSpec with MustMatchers {
  behavior of "If-Matches-Regex Transformer"

  val t = IfMatchesRegexTransformer("[abc]{2}")
  val CORRECT_VAL = "should be taken"
  val INCORRECT_VAL = "and not this one"

  it must "return the second input if regex matches any value" in {
    val inputs = Seq(
      Seq("black"),
      Seq(CORRECT_VAL),
      Seq(INCORRECT_VAL)
    )
    t(inputs) mustBe Seq(CORRECT_VAL)
  }

  it must "return the third input if regex does not match any value" in {
    val inputs = Seq(
      Seq("xyz"),
      Seq(INCORRECT_VAL),
      Seq(CORRECT_VAL)
    )
    t(inputs) mustBe Seq(CORRECT_VAL)
  }

  it must "return an empty result if regex does not match any value and only two inputs are given" in {
    val inputs = Seq(
      Seq("xyz"),
      Seq(INCORRECT_VAL)
    )
    t(inputs) mustBe Seq()
  }
}
