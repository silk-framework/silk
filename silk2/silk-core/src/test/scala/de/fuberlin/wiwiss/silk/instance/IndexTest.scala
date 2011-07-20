package de.fuberlin.wiwiss.silk.instance

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec

class IndexTest extends FlatSpec with ShouldMatchers {
  val a1 = Index.build(Set(1, 2, 3))
  val a2 = Index.build(Set(3, 4, 5))
  val a3 = Index.build(Set(4, 5, 6))

  "Index" should "pass simple tests" in {
    a1 matches a2 should equal(true)
    a1 matches a3 should equal(false)
  }

  val b1 = Index.build(Set(Int.MaxValue))
  val b2 = Index.build(Set(0))
  val b3 = Index.build(Set(0, Int.MaxValue))

  "Index" should "work with big numbers" in {
    b1 matches b1 should equal(true)
    b1 matches b2 should equal(false)
    b1 matches b3 should equal(true)
    b2 matches b3 should equal(true)
  }

  val c1 = Index.build((0 to 100 by 2).toSet)
  val c2 = Index.build((1 to 101 by 2).toSet)

  "Index" should "work with big indices" in {
    c1 matches c1 should equal(true)
    c1 matches c2 should equal(false)
  }
}
