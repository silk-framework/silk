package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.transformer.StripPostfixTransformer

class StripPostfixTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = new StripPostfixTransformer(Map("postfix" -> "abc"))

    "StripPostfixTransfomer" should "return 'abc123'" in
    {
        transformer.evaluate(List("abc123")) should equal ("abc123")
    }

    val transformer1 = new StripPostfixTransformer(Map("postfix" -> "123"))

    "StripPostfixTransfomer" should "return 'abc'" in
    {
        transformer1.evaluate(List("abc123")) should equal ("abc")
    }
}