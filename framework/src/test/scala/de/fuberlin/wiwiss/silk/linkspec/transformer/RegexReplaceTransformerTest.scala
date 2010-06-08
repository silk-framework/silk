package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.transformer.RegexReplaceTransformer

class RegexReplaceTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = new RegexReplaceTransformer(Map("regex" -> "[^0-9]*", "replace" -> ""))

    "RegexReplaceTransformerTest" should "return 'abc'" in
    {
        transformer.evaluate(List("a0b1c2")) should equal ("012")
    }

    val transformer1 = new RegexReplaceTransformer(Map("regex" -> "[a-z]*", "replace" -> ""))

    "RegexReplaceTransformerTest" should "return '1'" in
    {
        transformer1.evaluate(List("abcdef1")) should equal ("1")
    }
}