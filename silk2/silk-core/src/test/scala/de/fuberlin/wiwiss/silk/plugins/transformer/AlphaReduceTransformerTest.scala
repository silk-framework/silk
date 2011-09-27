package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins

class AlphaReduceTransformerTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val transformer = new AlphaReduceTransformer()

  "AlphaReduceTransformer" should "return 'abc'" in {
    transformer.evaluate("a1b0c") should equal("abc")
  }

  "AlphaReduceTransformer" should "return 'def'" in {
    transformer.evaluate("-def-") should equal("def")
  }
}