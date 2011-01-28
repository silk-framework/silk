package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.FlatSpec
import org.scalatest.matchers.{ShouldMatchers}
import de.fuberlin.wiwiss.silk.impl.transformer.StemmerTransformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class StemmerTransformerTest extends FlatSpec with ShouldMatchers
{
    DefaultImplementations.register()

    val transformer = new StemmerTransformer()

    "StemmerTransformer" should "return 'abolish'" in
    {
        transformer.evaluate(List("abolished")) should equal ("abolish")
    }

    val transformer1 = new StemmerTransformer()

    "StemmerTransformer" should "return 'abomin'" in
    {
        transformer1.evaluate(List("abominations")) should equal ("abomin")
    }
}