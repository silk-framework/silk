package org.silkframework.util


import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Created by andreas on 1/12/16.
 */
class SampleUtilTest extends AnyFlatSpec with Matchers {
  behavior of "SampleUtil"

  private implicit val random: Random = Random

  it should "sample from a set close to uniformly" in {
    val values = (for(i <- 1 to 10) yield {
      times(i, 10000)
    }).flatten
    isCloseToUniform(values)
    println("")
    val sample = SampleUtil.sample(values.iterator, 10000, None)
    isCloseToUniform(sample)
  }

  it should "sample from a set close to uniformly with filter" in {
    val values = (for(i <- 1 to 10) yield {
      times(i, 20000)
    }).flatten
    isCloseToUniform(values)
    var counter = 0
    val filter: Int => Boolean = i => {
      // Throw away 50% of then entities
      counter += 1
      if(counter % 2 == 0) {
        true
      } else {
        false
      }
    }
    val sample = SampleUtil.sample(values.iterator, 10000, Some(filter))
    isCloseToUniform(sample)
  }

  it should "sample every position with the same probability" in {
    // Each distinct value appears exactly once, so the sample frequency of a value is the inclusion probability
    // of its position. A position dependent sampling probability shows up here, while a value based check cannot see it.
    for((inputSize, sampleSize) <- Seq((5, 2), (10, 3))) {
      val trials = 50000
      val counts = Array.fill(inputSize)(0)
      for(_ <- 1 to trials) {
        for(value <- SampleUtil.sample((0 until inputSize).iterator, sampleSize, None)) {
          counts(value) += 1
        }
      }
      val expected = sampleSize.toDouble / inputSize
      for((count, position) <- counts.zipWithIndex) {
        withClue(s"for position $position of $inputSize (sample size $sampleSize): ") {
          count.toDouble / trials shouldBe (expected +- 0.02)
        }
      }
    }
  }

  it should "take all values if the input set is smaller" in {
    val input = 1 to 10
    val sample = SampleUtil.sample(input.iterator, 20, None)
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
      valueRatio shouldBe (0.1 +- 0.015) // It's practically impossible that a value will be lower/larger
    }
  }
}
