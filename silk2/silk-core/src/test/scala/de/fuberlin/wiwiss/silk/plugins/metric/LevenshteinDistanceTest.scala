package de.fuberlin.wiwiss.silk.plugins.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class LevenshteinDistanceTest extends FlatSpec with ShouldMatchers {

  val metric = new LevenshteinDistance()

  "LevenshteinDistance" should "return distance 0 for equal strings" in {
    metric.evaluate("kitten", "kitten") should equal(0)
    metric.evaluate("sitting", "sitting", 0.0) should equal(0)
  }

  "LevenshteinDistance" should "return distance 3 (kitten, sitting)" in {
    metric.evaluate("kitten", "sitting") should equal(3)
    metric.evaluate("sitting", "kitten") should equal(3)
    metric.evaluate("kitten", "sitting", 3.0) should equal(3)
    metric.evaluate("sitting", "kitten", 3.0) should equal(3)
  }

  "LevenshteinDistance" should "return distance 3 (Saturday, Sunday)" in {
    metric.evaluate("Saturday", "Sunday") should equal(3)
    metric.evaluate("Sunday", "Saturday") should equal(3)
  }

  "LevenshteinDistance" should "index correctly" in {
    (metric.indexValue("Sunday", 3) intersect metric.indexValue("Saturday", 3)) should equal(true)
    (metric.indexValue("Sunday", 4) intersect metric.indexValue("Saturday", 4)) should equal(true)
  }

//  "LevenshteinDistance" should "index '0' to the first block" in {
//    metric.indexValue("0", 0.0) should equal(Set(Seq(0)))
//  }
//
//  "LevenshteinDistance" should "index 'zzzzz' to the last block" in {
//    metric.indexValue("zzzzz", 0.0) should equal(Set(Seq(metric.blockCounts(0.0).head - 1)))
//  }

  // Invalid if q > 1:
  //  it should "generate one more index than the edit distance" in
  //  {
  //    val DPair(source, target) = randomValues(10, 5)
  //    val distance = metric.evaluate(source, target)
  //
  //    metric.index(source, distance).size should equal(distance + 1)
  //    metric.index(target, distance).size should equal(distance + 1)
  //  }
}

//object LevenshteinDistanceTest {
//  def main(args: Array[String]) {
//    val metric = new LevenshteinDistance()
//
//    for (run <- 0 until 100) {
//      val startTime = System.currentTimeMillis
//
//      for (i <- 0 until 100) {
//        val values = Seq.fill(100)(randomValues(30, 10))
//
//        metric.apply(values.map(_.source), values.map(_.target), 5)
//      }
//
//      val time = System.currentTimeMillis - startTime
//      println("Time: " + time)
//    }
//  }
//
//  /**
//   * Generates a pair of random strings with a specified maximum edit distance.
//   */
//  private def randomValues(size: Int, maxDistance: Int): DPair[String] = {
//    val sourceStr = Random.alphanumeric.take(size).mkString
//
//    val targetStr = randomEdits(sourceStr, maxDistance)
//
//    DPair(sourceStr, targetStr)
//  }
//
//  private def randomEdits(str: String, maxDistance: Int): String = {
//    Traversable.iterate(str, maxDistance + 1)(randomEdit).last
//  }
//
//  /**
//   * Modifies a string with a random edit operation.
//   */
//  private def randomEdit(str: String): String = {
//    val (prefix, suffix) = str.splitAt(Random.nextInt(str.length - 1))
//
//    Random.nextInt(3) match {
//      //Deletion
//      case 0 => prefix + suffix.tail
//      //Insertion
//      case 1 => prefix + Random.alphanumeric.head + suffix
//      //Substitution
//      case 2 => prefix + Random.alphanumeric.head + suffix.tail
//    }
//  }
//}