package de.fuberlin.wiwiss.silk.plugins.transformer

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class StripUriPrefixTransformerTest extends FlatSpec with ShouldMatchers {
  Plugins.register()

  val transformer = new StripUriPrefixTransformer()

  "StripUriPrefixTransformer" should "return 'Apple'" in {
    transformer.evaluate("http://dbpedia.org/resource/Apple") should equal("Apple")
  }

  "StripUriPrefixTransformer" should "return 'Moon'" in {
    transformer.evaluate("http://dbpedia.org/resource#Moon") should equal("Moon")
  }
}