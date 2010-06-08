package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.transformer.ConcatTransformer

class ConcatTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = new ConcatTransformer(Map())

    "ConcatTransformer" should "return 'abcdef'" in
    {
        transformer.evaluate(List("abc", "def")) should equal ("abcdef")
    }

    val transformer1 = new ConcatTransformer(Map())

    "ConcatTransformer" should "return 'def123'" in
    {
        transformer1.evaluate(List("def", "123")) should equal ("def123")
    }
}