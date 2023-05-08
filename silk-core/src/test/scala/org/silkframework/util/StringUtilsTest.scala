package org.silkframework.util

import StringUtils._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StringUtilsTest extends AnyFlatSpec with Matchers {

  behavior of "StringUtils.toSentenceCase"

  it should "convert title case to sentence case" in {
    "Hello World".toSentenceCase shouldBe "Hello world"
  }

  it should "undo camel case" in {
    "helloWorld".toSentenceCase shouldBe "Hello world"
  }

  it should "retain abbreviations" in {
    "keepABC DEF".toSentenceCase shouldBe "Keep ABC DEF"
  }

  it should "split before the last upper case character before a lower case character" in {
    "XMLError".toSentenceCase shouldBe "XML error"
  }
}
