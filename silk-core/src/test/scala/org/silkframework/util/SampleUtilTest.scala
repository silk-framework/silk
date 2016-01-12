package org.silkframework.util

import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by andreas on 1/12/16.
 */
class SampleUtilTest extends FlatSpec with Matchers {
  behavior of "SampleUtil"

  it should "sample from a set close to uniformly" in {
    val values = (for(i <- 1 to 10) yield {
      times(i, 10000)
    }).flatten
    isCloseToUniform(values)
    println("")
    val sample = SampleUtil.sample(values, 10000)
    isCloseToUniform(sample)
  }

  it should "take all value if the input set is smaller" in {
    val input = 1 to 10
    val sample = SampleUtil.sample(input, 20)
    sample.size shouldBe 10
    sample shouldBe (1 to 10)
  }

  def times(int: Int, nr: Int): Seq[Int] = {
    for(i <- 1 to nr) yield {
      int
    }
  }

  def isCloseToUniform(sample: Seq[Int]): Unit = {
    val sampleByValue = sample.groupBy(a => a)
    val overallSize = sample.size
    for((k, vals) <- sampleByValue.toSeq.sortWith(_._1 < _._1)) {
      val valueRatio = vals.size.toDouble / overallSize
      valueRatio shouldBe (0.1 +- 0.01) // It's practically impossible that a value will be lower/larger
    }
  }
}
