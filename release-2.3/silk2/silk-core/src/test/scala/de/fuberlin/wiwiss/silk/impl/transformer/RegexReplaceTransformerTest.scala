package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.transformer.RegexReplaceTransformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class RegexReplaceTransformerTest extends FlatSpec with ShouldMatchers
{
    DefaultImplementations.register()

    val transformer = new RegexReplaceTransformer(regex = "[^0-9]*", replace = "")

    "RegexReplaceTransformerTest" should "return 'abc'" in
    {
        transformer.evaluate(List("a0b1c2")) should equal ("012")
    }

    val transformer1 = new RegexReplaceTransformer(regex = "[a-z]*", replace = "")

    "RegexReplaceTransformerTest" should "return '1'" in
    {
        transformer1.evaluate(List("abcdef1")) should equal ("1")
    }
}