package de.fuberlin.wiwiss.silk.impl.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.util.SourceTargetPair
import util.Random

class LevenshteinDistanceTest extends FlatSpec with ShouldMatchers
{
  val metric = new LevenshteinDistance()

  "LevenshteinDistance" should "return distance 3 (kitten, sitting)" in
  {
    metric.evaluate("kitten", "sitting") should equal (3)
    metric.evaluate("sitting", "kitten") should equal (3)
  }

  "LevenshteinDistance" should "return distance 3 (Saturday, Sunday)" in
  {
    metric.evaluate("Saturday", "Sunday") should equal (3)
    metric.evaluate("Sunday", "Saturday") should equal (3)
  }

  "LevenshteinDistance" should "index in one dimension" in
  {
    metric.blockCounts(0.0).size should equal (1)
  }

  "LevenshteinDistance" should "index '0' to the first block" in
  {
    metric.index("0", 0.0) should equal (Set(Seq(0)))
  }

  "LevenshteinDistance" should "index 'zzzzz' to the last block" in
  {
    metric.index("zzzzz", 0.0) should equal (Set(Seq(metric.blockCounts(0.0).head - 1)))
  }

  it should "generate one more index than the edit distance" in
  {
    val SourceTargetPair(source, target) = randomValues(5)
    val distance = metric.evaluate(source, target)

    metric.index(source, distance).size should equal(distance + 1)
    metric.index(target, distance).size should equal(distance + 1)
  }

  /**
   * Generates a pair of random strings with a specified maximum edit distance.
   */
  private def randomValues(maxDistance : Int) : SourceTargetPair[String] =
  {
    val sourceStr = Random.alphanumeric.take(10).mkString

    val targetStr = Traversable.iterate(sourceStr, maxDistance + 1)(randomEdit).last

    SourceTargetPair(sourceStr, targetStr)
  }

  /**
   * Modifies a string with a random edit operation.
   */
  private def randomEdit(str : String) : String =
  {
    val (prefix, suffix) = str.splitAt(Random.nextInt(str.length - 1))

    Random.nextInt(3) match
    {
      //Deletion
      case 0 => prefix + suffix.tail
      //Insertion
      case 1 => prefix + Random.alphanumeric.head + suffix
      //Substitution
      case 2 => prefix + Random.alphanumeric.head + suffix.tail
    }
  }
}

//object LevenshteinDistanceTest
//{
//  def main(args : Array[String])
//  {
//    val metric = new LevenshteinDistance()
//
//    for(i <- 0 until 20)
//    {
//      val SourceTargetPair(s, t) = generateValues(5)
//      println("--------------------------------------------------------------")
//      println("Values: " + s + ", " + t)
//      println("Distance: " + metric.evaluate(s, t))
//      println("Blocks: " + metric.blockCounts.head)
//      println("Indices:" + metric.index(s, 5.0) + "," + metric.index(t, 5.0))
//    }
//  }
//
//
//}