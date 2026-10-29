package org.silkframework.runtime.plugin.types

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CompareOrderTest extends AnyFlatSpec with Matchers {

  behavior of "CompareOrder"

  it should "compare numbers numerically in autodetect order" in {
    CompareOrder.autodetect.isLower("2", "10", false) shouldBe true
    CompareOrder.autodetect.isLower("10", "2", false) shouldBe false
    CompareOrder.autodetect.isLower("0.5", "0.51", false) shouldBe true
    CompareOrder.autodetect.isLower("-1", "1", false) shouldBe true
  }

  it should "fall back to alphabetical comparison in autodetect order if either value is not a number" in {
    CompareOrder.autodetect.isLower("apple", "banana", false) shouldBe true
    CompareOrder.autodetect.isLower("banana", "apple", false) shouldBe false
    // Alphabetically, "10" is lower than "a" and "10x" is lower than "2"
    CompareOrder.autodetect.isLower("10", "a", false) shouldBe true
    CompareOrder.autodetect.isLower("2", "10x", false) shouldBe false
  }

  it should "only accept equal values if orEqual is set" in {
    for(order <- Seq(CompareOrder.autodetect, CompareOrder.alphabetical, CompareOrder.numerical, CompareOrder.integer)) {
      order.isLower("2", "2", false) shouldBe false
      order.isLower("2", "2", true) shouldBe true
    }
    CompareOrder.autodetect.isLower("a", "a", false) shouldBe false
    CompareOrder.autodetect.isLower("a", "a", true) shouldBe true
    // Numerically equal, although not the same string
    CompareOrder.numerical.isLower("2.0", "2", true) shouldBe true
  }

  it should "always compare values as strings in alphabetical order" in {
    CompareOrder.alphabetical.isLower("10", "2", false) shouldBe true
    CompareOrder.alphabetical.isLower("2", "10", false) shouldBe false
  }

  it should "never prefer values that cannot be parsed in numerical order" in {
    CompareOrder.numerical.isLower("2", "10", false) shouldBe true
    CompareOrder.numerical.isLower("a", "10", false) shouldBe false
    CompareOrder.numerical.isLower("10", "a", false) shouldBe false
    CompareOrder.numerical.isLower("a", "b", true) shouldBe false
  }

  it should "only accept whole numbers in integer order" in {
    CompareOrder.integer.isLower("2", "10", false) shouldBe true
    CompareOrder.integer.isLower("10", "2", false) shouldBe false
    CompareOrder.integer.isLower("1.5", "2", false) shouldBe false
    CompareOrder.integer.isLower("2", "1.5", false) shouldBe false
  }
}
