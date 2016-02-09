package org.silkframework.entity



import org.scalatest.FlatSpec
import org.scalatest.Matchers


class IndexTest extends FlatSpec with Matchers {

  "Index" should "combine single dimension indices disjunctively by offseting the second index values" in {
    Index.oneDim(Set(0), 10) disjunction Index.oneDim(Set(0), 10) should equal (Index.oneDim(Set(0, 10), 20))
    Index.oneDim(Set(1,4), 5) disjunction Index.oneDim(Set(3,9), 10) should equal (Index.oneDim(Set(1,4,8,14), 15))
    Index.oneDim(Set(0,99), 100) disjunction Index.oneDim(Set(0,9), 10) should equal (Index.oneDim(Set(0,99,100,109), 110))
  }

  "Index" should "combine multi dimensional indices disjunctively by offseting the second index values" in {
    val i1 = Index.multiDim(Set(Seq(0,0)), Seq(10,20))
    val i2 = Index.multiDim(Set(Seq(0,0),Seq(99,199)), Seq(100,200))
    val i3 = Index.multiDim(Set(Seq(0,0),Seq(10,20),Seq(109,219)), Seq(110,220))

    i1 disjunction i2 should equal (i3)
  }

  "Index" should "combine single and multi dimension indices disjunctively by padding the single dimension index" in {
    val i1 = Index.oneDim(Set(0), 10)
    val i2 = Index.multiDim(Set(Seq(0,0)), Seq(20,40))

    val r1 = Index.multiDim(Set(Seq(0,0),Seq(10,1)), Seq(30,41))
    val r2 = Index.multiDim(Set(Seq(0,0),Seq(20,40)), Seq(30,41))

    i1 disjunction i2 should equal (r1)
    i2 disjunction i1 should equal (r2)
  }
}
