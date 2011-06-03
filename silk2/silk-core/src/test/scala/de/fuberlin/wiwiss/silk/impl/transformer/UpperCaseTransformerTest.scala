package de.fuberlin.wiwiss.silk.impl.transformer

import org.scalatest.FlatSpec
import org.scalatest.matchers.{ShouldMatchers}
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class UpperCaseTransformerTest extends FlatSpec with ShouldMatchers
{
  DefaultImplementations.register()

  val transformer = new UpperCaseTransformer()

  "UpperCaseTransformer" should "return '123'" in
  {
    transformer.evaluate("123") should equal ("123")
  }

  val transformer1 = new UpperCaseTransformer()

  "UpperCaseTransformer" should "return 'ABC'" in
  {
    transformer1.evaluate("abc") should equal ("ABC")
  }
}