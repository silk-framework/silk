package org.silkframework.rule.plugins.transformer.conditional

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.test.PluginTest

/**
  * Created on 9/9/16.
  */
class IfMatchesRegexTransformerTest extends PluginTest {

  val t = IfMatchesRegexTransformer("[abc]{2}")
  val CORRECT_VAL = "should be taken"
  val INCORRECT_VAL = "and not this one"

  it must "return the second input if regex matches any value" in {
    val inputs = Seq(
      Seq("black"),
      Seq(CORRECT_VAL),
      Seq(INCORRECT_VAL)
    )
    t(inputs) shouldBe Seq(CORRECT_VAL)
  }

  it must "return the third input if regex does not match any value" in {
    val inputs = Seq(
      Seq("xyz"),
      Seq(INCORRECT_VAL),
      Seq(CORRECT_VAL)
    )
    t(inputs) shouldBe Seq(CORRECT_VAL)
  }

  it must "return an empty result if regex does not match any value and only two inputs are given" in {
    val inputs = Seq(
      Seq("xyz"),
      Seq(INCORRECT_VAL)
    )
    t(inputs) shouldBe Seq()
  }

  override def pluginObject = IfMatchesRegexTransformer("[abc]{2}")
}
