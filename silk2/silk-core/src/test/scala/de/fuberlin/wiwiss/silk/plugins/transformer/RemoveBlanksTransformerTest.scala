package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.DefaultPlugins

class RemoveBlanksTransformerTest extends FlatSpec with ShouldMatchers {
  DefaultPlugins.register()

  val transformer = new RemoveBlanksTransformer()

  "RemoveBlanksTransformer" should "return 'abc'" in {
    transformer.evaluate("a b c") should equal("abc")
  }
}