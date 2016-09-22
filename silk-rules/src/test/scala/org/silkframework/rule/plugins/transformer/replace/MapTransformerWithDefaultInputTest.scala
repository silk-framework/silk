package org.silkframework.rule.plugins.transformer.replace

import org.scalatest.{MustMatchers, FlatSpec}

/**
  * Created on 9/1/16.
  */
class MapTransformerWithDefaultInputTest extends FlatSpec with MustMatchers {
  behavior of "map transformer with default input"
  final val A = "A"
  final val B = "B"
  final val Nr1 = "1"
  final val Nr2 = "2"

  it should "fail with illegal arguments" in {
    val t = MapTransformerWithDefaultInput(Map(A -> Nr1, B -> Nr2))
    val wrongNumberOfInputs = Seq(
      Seq(Seq(A), Seq(B), Seq("C")),
      Seq(Seq(A)),
      Seq()
    )
    for(input <- wrongNumberOfInputs) {
      intercept[IllegalArgumentException] {
        t(input)
      }
    }
  }

  it should "fail if no defaults are given" in {
    val t = MapTransformerWithDefaultInput(Map(A -> Nr1, B -> Nr2))
    val noDefaultInput = Seq(Seq(A), Seq())
    intercept[IllegalArgumentException] {
      t(noDefaultInput)
    }
  }

  it should "fill in the defaults if less defaults are given" in {
    val t = MapTransformerWithDefaultInput(Map(A -> Nr1, B -> Nr2))
    val lessDefaults = Seq(Seq(A, B, "c"), Seq("default"))
    t(lessDefaults) mustBe Seq(Nr1, Nr2, "default")
  }
}
