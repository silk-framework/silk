package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.DefaultPlugins

class StripPostfixTransformerTest extends FlatSpec with ShouldMatchers {
  DefaultPlugins.register()

  val transformer = new StripPostfixTransformer(postfix = "abc")

  "StripPostfixTransfomer" should "return 'abc123'" in {
    transformer.evaluate("abc123") should equal("abc123")
  }

  val transformer1 = new StripPostfixTransformer(postfix = "123")

  "StripPostfixTransfomer" should "return 'abc'" in {
    transformer1.evaluate("abc123") should equal("abc")
  }
}