package org.silkframework.rule.plugins.transformer.sequence

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import GetValueByIndexTransformer.{apply => get}

/**
  * Verifies GetValueByIndexTransformer's index-lookup contract: positive and negative indices, and their interaction
  * with failIfNotFound and emptyStringToEmptyResult.
  */
class GetValueByIndexTransformerTest extends AnyFlatSpec with Matchers {
  import GetValueByIndexTransformerTest._

  behavior of "get value by index transformer"

  it should "Get the right value by index" in {
    get(0)(Values.ONE_TWO) shouldBe Elements.ONE
    get(1)(Values.ONE_TWO) shouldBe Elements.TWO
    get(2)(Values.ONE_TWO) shouldBe Elements.EMPTY
  }

  it should "throw IndexOutOfBoundsException if failIfNotFound is set and there is no value at index" in {
    intercept[IndexOutOfBoundsException] {
      get(1, failIfNotFound = true)(Values.ONE)
    }
  }

  it should "return an empty result for an empty String if emptyStringToEmptyResult==true" in {
    get(0, emptyStringToEmptyResult = true)(Values.EMPTY_STRING) shouldBe Elements.EMPTY
  }

  it should "return the correct value for negative indices, counting from the end" in {
    get(-1)(Values.ONE_TWO_THREE) shouldBe Elements.THREE
    get(-2)(Values.ONE_TWO_THREE) shouldBe Elements.TWO
    get(-3)(Values.ONE_TWO_THREE) shouldBe Elements.ONE
  }

  it should "return the correct value for a single-element sequence, where the last-element and wrap-to-start cases coincide" in {
    get(-1)(Values.ONE) shouldBe Elements.ONE
  }

  it should "return an empty result for a negative index out of range, or for an empty input sequence" in {
    get(-4)(Values.ONE_TWO_THREE) shouldBe Elements.EMPTY
    get(0)(Values.EMPTY) shouldBe Elements.EMPTY
    get(-1)(Values.EMPTY) shouldBe Elements.EMPTY
  }

  it should "throw IndexOutOfBoundsException if failIfNotFound is set and a negative index is out of range" in {
    intercept[IndexOutOfBoundsException] {
      get(-4, failIfNotFound = true)(Values.ONE_TWO_THREE)
    }
  }

  it should "not throw for a negative index exactly at the wrap-to-start boundary, even if failIfNotFound is set" in {
    get(-3, failIfNotFound = true)(Values.ONE_TWO_THREE) shouldBe Elements.ONE
  }

  it should "return an empty result for an empty String at a negative index if emptyStringToEmptyResult==true" in {
    get(-1)(Values.ONE_EMPTY_STRING) shouldBe Elements.EMPTY_STRING
    get(-1, emptyStringToEmptyResult = true)(Values.ONE_EMPTY_STRING) shouldBe Elements.EMPTY
  }

  it should "resolve a negative index independently for each input sequence" in {
    get(-1)(Seq(Elements.ONE_TWO_THREE, Elements.ONE_TWO)) shouldBe Seq(Strings.THREE, Strings.TWO)
  }

  it should "not overflow for extreme index values" in {
    get(Int.MinValue)(Values.ONE_TWO_THREE) shouldBe Elements.EMPTY
    get(Int.MaxValue)(Values.ONE_TWO_THREE) shouldBe Elements.EMPTY
  }
}

object GetValueByIndexTransformerTest {

  object Strings {
    val ONE: String = "1"
    val TWO: String = "2"
    val THREE: String = "3"
    val EMPTY: String = ""
  }

  object Elements {
    val EMPTY: Seq[String] = Seq[String]()
    val ONE: Seq[String] = Seq(Strings.ONE)
    val TWO: Seq[String] = Seq(Strings.TWO)
    val THREE: Seq[String] = Seq(Strings.THREE)
    val ONE_TWO: Seq[String] = Seq(Strings.ONE, Strings.TWO)
    val ONE_TWO_THREE: Seq[String] = Seq(Strings.ONE, Strings.TWO, Strings.THREE)
    val EMPTY_STRING: Seq[String] = Seq(Strings.EMPTY)
    val ONE_EMPTY_STRING: Seq[String] = Seq(Strings.ONE, Strings.EMPTY)
  }

  object Values {
    val EMPTY: Seq[Seq[String]] = Seq(Elements.EMPTY)
    val ONE: Seq[Seq[String]] = Seq(Elements.ONE)
    val ONE_TWO: Seq[Seq[String]] = Seq(Elements.ONE_TWO)
    val ONE_TWO_THREE: Seq[Seq[String]] = Seq(Elements.ONE_TWO_THREE)
    val EMPTY_STRING: Seq[Seq[String]] = Seq(Elements.EMPTY_STRING)
    val ONE_EMPTY_STRING: Seq[Seq[String]] = Seq(Elements.ONE_EMPTY_STRING)
  }
}
