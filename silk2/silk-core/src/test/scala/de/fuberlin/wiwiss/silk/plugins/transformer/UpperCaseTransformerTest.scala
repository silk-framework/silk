package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.DefaultPlugins

class UpperCaseTransformerTest extends FlatSpec with ShouldMatchers {
  DefaultPlugins.register()

  val transformer = new UpperCaseTransformer()

  "UpperCaseTransformer" should "return '123'" in {
    transformer.evaluate("123") should equal("123")
  }

  val transformer1 = new UpperCaseTransformer()

  "UpperCaseTransformer" should "return 'ABC'" in {
    transformer1.evaluate("abc") should equal("ABC")
  }
}