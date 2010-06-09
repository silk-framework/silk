package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import de.fuberlin.wiwiss.silk.linkspec.Transformer
import org.scalatest.FlatSpec

class RemoveSpecialCharsTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = Transformer("removeSpecialChars", Map())

    "RemoveSpecialCharsTransformer" should "return 'abc'" in
    {
        transformer.evaluate(List("a.b.c-")) should equal ("abc")
    }
}