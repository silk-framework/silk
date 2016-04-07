package org.silkframework.plugins.distance.characterbased

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created on 4/5/16.
  */
class SubStringDistanceTest extends FlatSpec with Matchers {
  behavior of "SubStringDistance"

  val substring = new SubStringDistance()
  def eval(str1: String, str2: String) = substring.evaluate(str1, str2, 1.0)

  it should "calculate correct relative distances " in {
    val str1 = "ABCDEF"
    val str2 = str1 + "123"
    val str3 = str1 + "4567"
    eval(str1, str2) shouldBe < (eval(str1, str3))
    eval(str1, str3) shouldBe < (eval(str2, str3))
  }

  it should "use the granularity correctly" in {
    val s = new SubStringDistance("2")
    val str1 = "AB12"
    val str2 = "12AB"
    substring.indexValue(str1, 1.0) should not be (s.indexValue(str1, 1.0))
    eval(str1, str2) shouldBe 1.0
    s.evaluate(str1, str2) shouldBe 0.0
  }
}
