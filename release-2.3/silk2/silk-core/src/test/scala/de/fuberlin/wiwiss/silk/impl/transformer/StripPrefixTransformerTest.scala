package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.transformer.StripPrefixTransformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class StripPrefixTransformerTest extends FlatSpec with ShouldMatchers
{
    DefaultImplementations.register()

    val transformer = new StripPrefixTransformer(prefix = "abc")

    "StripPrefixTransformer" should "return '123'" in
    {
        transformer.evaluate(List("abc123")) should equal ("123")
    }

    val transformer1 = new StripPrefixTransformer(prefix = "123")

    "StripPrefixTransformer" should "return 'abc'" in
    {
        transformer1.evaluate(List("123abc")) should equal ("abc")
    }
}