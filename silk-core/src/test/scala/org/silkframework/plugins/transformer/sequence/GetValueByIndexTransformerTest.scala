package org.silkframework.plugins.transformer.sequence

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created on 6/9/16.
  */
class GetValueByIndexTransformerTest extends FlatSpec with Matchers {
  behavior of "get value by index transformer"

  it should "Get the right value by index" in {
    get(Seq(Seq("1", "2")), 0) shouldBe Seq("1")
    get(Seq(Seq("1", "2")), 1) shouldBe Seq("2")
    get(Seq(Seq("1", "2")), 2) shouldBe Seq()
  }

  it should "should throw IndexOutOfBoundsException if failIfNotFound is set and there is no value at index" in {
    intercept[IndexOutOfBoundsException] {
      get(Seq(Seq("1")), 1, failIfNotFound = true)
    }
  }

  private def get(values: Seq[Seq[String]],
                  index: Int,
                  failIfNotFound: Boolean = false): Seq[String] = {
    val tr = new GetValueByIndexTransformer(index, failIfNotFound)
    tr(values)
  }
}
