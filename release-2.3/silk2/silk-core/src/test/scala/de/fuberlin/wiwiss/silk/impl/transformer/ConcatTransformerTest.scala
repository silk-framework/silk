package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.transformer.ConcatTransformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class ConcatTransformerTest extends FlatSpec with ShouldMatchers
{
  DefaultImplementations.register()

  val transformer = new ConcatTransformer()

  "ConcatTransformer" should "return 'abcdef'" in
  {
    transformer.evaluate(List("abc", "def")) should equal ("abcdef")
  }

  val transformer1 = new ConcatTransformer()

  "ConcatTransformer" should "return 'def123'" in
  {
    transformer1.evaluate(List("def", "123")) should equal ("def123")
  }
}
