package org.silkframework.rule

import org.silkframework.config.Prefixes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UriPatternTest extends AnyFlatSpec with Matchers {

  behavior of "UriPattern"

  it should "parse basic patterns" in {
    testRoundtrip("http://example.org/namespace/entity{id}-{date}")
  }

  it should "parse patterns with no prefix" in {
    testRoundtrip("{}suffix")
    testRoundtrip("{uri}suffix")
  }

  def testRoundtrip(uriPattern: String): Unit = {
    implicit val prefixes: Prefixes = Prefixes.empty
    val input = UriPattern.parse(uriPattern)
    UriPattern.unapply(input) shouldBe Some(uriPattern)
  }
}
