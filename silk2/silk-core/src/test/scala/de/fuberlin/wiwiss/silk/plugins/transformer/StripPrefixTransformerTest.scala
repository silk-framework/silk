package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.DefaultPlugins

class StripPrefixTransformerTest extends FlatSpec with ShouldMatchers {
  DefaultPlugins.register()

  val transformer = new StripPrefixTransformer(prefix = "abc")

  "StripPrefixTransformer" should "return '123'" in {
    transformer.evaluate("abc123") should equal("123")
  }

  val transformer1 = new StripPrefixTransformer(prefix = "123")

  "StripPrefixTransformer" should "return 'abc'" in {
    transformer1.evaluate("123abc") should equal("abc")
  }
}