package de.fuberlin.wiwiss.silk.linkspec.transformer

import org.scalatest.matchers.{ShouldMatchers}
import de.fuberlin.wiwiss.silk.transformer.StripUriPrefixTransformer
import org.scalatest.FlatSpec

class StripUriPrefixTransformerTest extends FlatSpec with ShouldMatchers
{
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