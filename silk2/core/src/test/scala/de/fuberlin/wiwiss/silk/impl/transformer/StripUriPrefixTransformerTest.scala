package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.impl.transformer.StripUriPrefixTransformer
import de.fuberlin.wiwiss.silk.impl.DefaultImplementations

class StripUriPrefixTransformerTest extends FlatSpec with ShouldMatchers
{
    DefaultImplementations.register()

    val transformer = new StripUriPrefixTransformer(Map())

    "StripUriPrefixTransformer" should "return 'Apple'" in
    {
        transformer.evaluate(List("http://dbpedia.org/resource/Apple")) should equal ("Apple")
    }

    "StripUriPrefixTransformer" should "return 'Moon'" in
    {
        transformer.evaluate(List("http://dbpedia.org/resource#Moon")) should equal ("Moon")
    }
}