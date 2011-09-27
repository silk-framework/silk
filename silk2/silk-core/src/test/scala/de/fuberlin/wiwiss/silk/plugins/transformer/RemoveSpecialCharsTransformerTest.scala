package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins

class RemoveSpecialCharsTransformerTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val transformer = new RemoveSpecialCharsTransformer()

  "RemoveSpecialCharsTransformer" should "return 'abc'" in {
    transformer.evaluate("a.b.c-") should equal("abc")
  }
}