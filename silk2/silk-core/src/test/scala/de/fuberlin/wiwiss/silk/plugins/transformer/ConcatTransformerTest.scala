package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConcatTransformerTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val transformer = new ConcatTransformer()

  "ConcatTransformer" should "return 'abcdef'" in {
    transformer.apply(Seq(Set("abc"), Set("def"))) should equal(Set("abcdef"))
  }

  val transformer1 = new ConcatTransformer()

  "ConcatTransformer" should "return 'def123'" in {
    transformer1.apply(Seq(Set("def"), Set("123"))) should equal(Set("def123"))
  }
}
