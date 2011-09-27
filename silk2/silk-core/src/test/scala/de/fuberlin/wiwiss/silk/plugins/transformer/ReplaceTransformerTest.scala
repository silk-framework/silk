package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins

class ReplaceTransformerTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val transformer = new ReplaceTransformer(search = " ", replace = "")

  "ReplaceTransformer" should "return 'abc'" in {
    transformer.evaluate("a b c") should equal("abc")
  }

  val transformer1 = new ReplaceTransformer(search = "abc", replace = "")

  "ReplaceTransformer" should "return 'def'" in {
    transformer1.evaluate("abcdef") should equal("def")
  }
}