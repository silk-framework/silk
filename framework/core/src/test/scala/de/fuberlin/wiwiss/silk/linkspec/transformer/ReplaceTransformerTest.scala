package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.transformer.ReplaceTransformer

class ReplaceTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = new ReplaceTransformer(Map("search" -> " ", "replace" -> ""))

    "ReplaceTransformer" should "return 'abc'" in
    {
        transformer.evaluate(List("a b c")) should equal ("abc")
    }

    val transformer1 = new ReplaceTransformer(Map("search" -> "abc", "replace" -> ""))

    "ReplaceTransformer" should "return 'def'" in
    {
        transformer1.evaluate(List("abcdef")) should equal ("def")
    }
}