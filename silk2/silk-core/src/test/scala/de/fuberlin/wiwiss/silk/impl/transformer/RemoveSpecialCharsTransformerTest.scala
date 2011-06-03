package de.fuberlin.wiwiss.silk.impl.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class RemoveSpecialCharsTransformerTest extends FlatSpec with ShouldMatchers
{
  DefaultImplementations.register()

  val transformer = new RemoveSpecialCharsTransformer()

  "RemoveSpecialCharsTransformer" should "return 'abc'" in
  {
    transformer.evaluate("a.b.c-") should equal ("abc")
  }
}