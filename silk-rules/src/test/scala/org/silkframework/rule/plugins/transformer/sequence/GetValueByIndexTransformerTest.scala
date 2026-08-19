package org.silkframework.rule.plugins.transformer.sequence

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Created on 6/9/16.
  */
class GetValueByIndexTransformerTest extends AnyFlatSpec with Matchers {
  behavior of "get value by index transformer"

  private val ONE = "1"
  private val TWO = "2"
  private val THREE = "3"

  it should "Get the right value by index" in {
    get(Seq(Seq(ONE, TWO)), 0) shouldBe Seq(ONE)
    get(Seq(Seq(ONE, TWO)), 1) shouldBe Seq(TWO)
    get(Seq(Seq(ONE, TWO)), 2) shouldBe Seq()
  }

  it should "throw IndexOutOfBoundsException if failIfNotFound is set and there is no value at index" in {
    intercept[IndexOutOfBoundsException] {
      get(Seq(Seq(ONE)), 1, failIfNotFound = true)
    }
  }

  it should "return an empty result for an empty String if emptyStringToEmptyResult==true" in {
    get(Seq(Seq("")), 0, emptyStringToEmptyResult = true) shouldBe Seq()
  }

  it should "return the correct value for negative indices, counting from the end" in {
    get(Seq(Seq(ONE, TWO, THREE)), -1) shouldBe Seq(THREE)
    get(Seq(Seq(ONE, TWO, THREE)), -2) shouldBe Seq(TWO)
    get(Seq(Seq(ONE, TWO, THREE)), -3) shouldBe Seq(ONE)
    get(Seq(Seq(ONE)), -1) shouldBe Seq(ONE)
  }

  it should "return an empty result for a negative index out of range, or for an empty input sequence" in {
    get(Seq(Seq(ONE, TWO, THREE)), -4) shouldBe Seq()
    get(Seq(Seq()), 0) shouldBe Seq()
    get(Seq(Seq()), -1) shouldBe Seq()
  }

  it should "throw IndexOutOfBoundsException if failIfNotFound is set and a negative index is out of range" in {
    intercept[IndexOutOfBoundsException] {
      get(Seq(Seq(ONE, TWO, THREE)), -4, failIfNotFound = true)
    }
  }

  it should "not throw for a negative index exactly at the wrap-to-start boundary, even if failIfNotFound is set" in {
    get(Seq(Seq(ONE, TWO, THREE)), -3, failIfNotFound = true) shouldBe Seq(ONE)
  }

  it should "return an empty result for an empty String at a negative index if emptyStringToEmptyResult==true" in {
    get(Seq(Seq(ONE, "")), -1, emptyStringToEmptyResult = false) shouldBe Seq("")
    get(Seq(Seq(ONE, "")), -1, emptyStringToEmptyResult = true) shouldBe Seq()
  }

  it should "resolve a negative index independently for each input sequence" in {
    get(Seq(Seq(ONE, TWO, THREE), Seq(ONE, TWO)), -1) shouldBe Seq(THREE, TWO)
  }

  it should "not overflow for extreme index values" in {
    get(Seq(Seq(ONE, TWO, THREE)), Int.MinValue) shouldBe Seq()
    get(Seq(Seq(ONE, TWO, THREE)), Int.MaxValue) shouldBe Seq()
  }

  private def get(values: Seq[Seq[String]],
                  index: Int,
                  failIfNotFound: Boolean = false,
                  emptyStringToEmptyResult: Boolean = false): Seq[String] = {
    val tr = new GetValueByIndexTransformer(index, failIfNotFound, emptyStringToEmptyResult)
    tr(values)
  }
}
