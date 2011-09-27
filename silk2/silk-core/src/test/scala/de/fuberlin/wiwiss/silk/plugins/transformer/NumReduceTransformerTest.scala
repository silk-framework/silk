package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec

class NumReduceTransformerTest extends FlatSpec with ShouldMatchers {
  val transformer = new NumReduceTransformer()

  "NumReduceTransformer" should "return '10'" in {
    transformer.evaluate("a1b0c") should equal("10")
  }
}