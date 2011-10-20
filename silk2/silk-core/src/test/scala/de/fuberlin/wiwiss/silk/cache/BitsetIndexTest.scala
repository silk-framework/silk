package de.fuberlin.wiwiss.silk.cache

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BitsetIndexTest extends FlatSpec with ShouldMatchers {
  val a1 = BitsetIndex.build(Set(1, 2, 3))
  val a2 = BitsetIndex.build(Set(3, 4, 5))
  val a3 = BitsetIndex.build(Set(4, 5, 6))

  "Index" should "pass simple tests" in {
    a1 matches a2 should equal(true)
    a1 matches a3 should equal(false)
  }

  val b1 = BitsetIndex.build(Set(5000, 5001, 5002))
  val b2 = BitsetIndex.build(Set(5000))
  val b3 = BitsetIndex.build(Set(5003))

  "Index" should "work for big indexes" in {
    b1 matches b2 should equal(true)
    b1 matches b3 should equal(false)
  }

  val c1 = BitsetIndex.build(Set(Int.MaxValue))
  val c2 = BitsetIndex.build(Set(0))
  val c3 = BitsetIndex.build(Set(0, Int.MaxValue))

  "Index" should "work with big numbers" in {
    c1 matches c1 should equal(true)
    c1 matches c2 should equal(false)
    c1 matches c3 should equal(true)
    c2 matches c3 should equal(true)
  }

  val d1 = BitsetIndex.build((0 to 100 by 2).toSet)
  val d2 = BitsetIndex.build((1 to 101 by 2).toSet)

  "Index" should "work with big indices" in {
    d1 matches d1 should equal(true)
    d1 matches d2 should equal(false)
  }
}
