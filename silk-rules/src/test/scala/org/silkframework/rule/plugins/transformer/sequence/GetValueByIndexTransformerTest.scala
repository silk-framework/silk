package org.silkframework.rule.plugins.transformer.sequence

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created on 6/9/16.
  */
class GetValueByIndexTransformerTest extends FlatSpec with Matchers {
  behavior of "get value by index transformer"

  private val ONE = "1"
  private val TWO = "2"

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

  private def get(values: Seq[Seq[String]],
                  index: Int,
                  failIfNotFound: Boolean = false,
                  emptyStringToEmptyResult: Boolean = false): Seq[String] = {
    val tr = new GetValueByIndexTransformer(index, failIfNotFound, emptyStringToEmptyResult)
    tr(values)
  }
}
