package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.FlatSpec
import org.scalatest.matchers.{ShouldMatchers}
import de.fuberlin.wiwiss.silk.transformer.StemmerTransformer

class StemmerTransformerTest extends FlatSpec with ShouldMatchers
{
    val transformer = new StemmerTransformer(Map())

    "StemmerTransformer" should "return 'abolish'" in
    {
        transformer.evaluate(List("abolished")) should equal ("abolish")
    }

    val transformer1 = new StemmerTransformer(Map())

    "StemmerTransformer" should "return 'abomin'" in
    {
        transformer1.evaluate(List("abominations")) should equal ("abomin")
    }
}