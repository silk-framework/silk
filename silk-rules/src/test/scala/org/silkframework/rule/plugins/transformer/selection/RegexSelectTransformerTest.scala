package org.silkframework.rule.plugins.transformer.selection

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/**
  * Created on 9/9/16.
  */
class RegexSelectTransformerTest extends AnyFlatSpec with Matchers {
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
