package org.silkframework.evaluation

import org.scalatest.{Matchers, FlatSpec}

/**
  * Created by andreas on 2/22/16.
  */
class EvaluationResultTest extends FlatSpec with Matchers {
  behavior of "EvaluationResult"

  it should "calculate correct values for true values only" in {
    val result = new EvaluationResult(truePositives = 1, trueNegatives = 9999, falsePositives = 0, falseNegatives = 0)
    result.precision shouldBe 1.0
    result.recall shouldBe 1.0
    result.fMeasure shouldBe 1.0
  }

  it should "calculate correct values for mixed values" in {
    val result = new EvaluationResult(truePositives = 1, trueNegatives = 9999, falsePositives = 1, falseNegatives = 1)
    result.precision shouldBe 0.5
    result.recall shouldBe 0.5
    result.fMeasure shouldBe 0.5
  }
}
