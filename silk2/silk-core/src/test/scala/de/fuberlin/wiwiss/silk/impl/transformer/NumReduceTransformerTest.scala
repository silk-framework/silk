package de.fuberlin.wiwiss.silk.impl.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class NumReduceTransformerTest extends FlatSpec with ShouldMatchers
{
  DefaultImplementations.register()

  val transformer = new NumReduceTransformer()

  "NumReduceTransformer" should "return '10'" in
  {
    transformer.evaluate("a1b0c") should equal ("10")
  }
}