package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins

class LowerCaseTransformerTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val transformer = new LowerCaseTransformer()

  "LowerCaseTransformer" should "return '123'" in {
    transformer.evaluate("123") should equal("123")
  }

  val transformer1 = new LowerCaseTransformer()

  "LowerCaseTransformer" should "return 'abc'" in {
    transformer1.evaluate("ABc") should equal("abc")
  }
}