package de.fuberlin.wiwiss.silk.impl.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class ConcatTransformerTest extends FlatSpec with ShouldMatchers {
  DefaultImplementations.register()

  val transformer = new ConcatTransformer()

  "ConcatTransformer" should "return 'abcdef'" in {
    transformer.apply(List(List("abc"), List("def"))) should equal("abcdef")
  }

  val transformer1 = new ConcatTransformer()

  "ConcatTransformer" should "return 'def123'" in {
    transformer1.apply(List(List("def"), List("123"))) should equal("def123")
  }
}
