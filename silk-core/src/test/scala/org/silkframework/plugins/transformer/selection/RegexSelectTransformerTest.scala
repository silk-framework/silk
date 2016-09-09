package org.silkframework.plugins.transformer.selection

import org.scalatest.{MustMatchers, FlatSpec}

/**
  * Created on 9/9/16.
  */
class RegexSelectTransformerTest extends FlatSpec with MustMatchers {
  behavior of "Regex Select Transformer"

  private val outputValue = "output"

  it must "Set the correct outputs based on the regexes" in {
    val t = RegexSelectTransformer()
    val inputs = Seq(
      Seq(outputValue),
      Seq("a", "b", "c"),
      Seq("catch")
    )
    t.apply(inputs) mustBe Seq(outputValue, "", outputValue)
  }

  it must "return only first match position if oneOnly==true" in {
    val t = RegexSelectTransformer(oneOnly = true)
    val inputs = Seq(
      Seq(outputValue),
      Seq("a", "b", "c"),
      Seq("catch")
    )
    t.apply(inputs) mustBe Seq(outputValue, "", "")
  }
}
