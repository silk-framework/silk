package org.silkframework.util

import org.scalatest.{FlatSpec, Matchers}
import StringUtils._

class StringUtilsTest extends FlatSpec with Matchers {{

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
}

}
