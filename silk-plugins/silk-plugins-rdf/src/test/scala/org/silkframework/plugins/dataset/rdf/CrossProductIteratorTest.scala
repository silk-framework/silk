package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.plugins.dataset.rdf.executors.CrossProductIterator

class CrossProductIteratorTest extends FlatSpec with MustMatchers {
  behavior of "Cross Product Matcher"

  it should "calculate the cross product" in {
    val it = CrossProductIterator(IndexedSeq(Seq("A", "B"), Seq("a"), Seq("1", "2", "3")), IndexedSeq("prop1", "prop2", "prop3"))
    it.toSeq mustBe Seq(
      Map("prop1" -> "A", "prop2" -> "a", "prop3" -> "1"),
      Map("prop1" -> "A", "prop2" -> "a", "prop3" -> "2"),
      Map("prop1" -> "A", "prop2" -> "a", "prop3" -> "3"),
      Map("prop1" -> "B", "prop2" -> "a", "prop3" -> "1"),
      Map("prop1" -> "B", "prop2" -> "a", "prop3" -> "2"),
      Map("prop1" -> "B", "prop2" -> "a", "prop3" -> "3")
    )
  }

  it should "leave out assignments with zero values" in {
    val it = CrossProductIterator(IndexedSeq(Seq("A", "B"), Seq(), Seq("1", "2", "3")), IndexedSeq("prop1", "prop2", "prop3"))
    it.toSeq mustBe Seq(
      Map("prop1" -> "A", "prop3" -> "1"),
      Map("prop1" -> "A", "prop3" -> "2"),
      Map("prop1" -> "A", "prop3" -> "3"),
      Map("prop1" -> "B", "prop3" -> "1"),
      Map("prop1" -> "B", "prop3" -> "2"),
      Map("prop1" -> "B", "prop3" -> "3")
    )
    val it2 = CrossProductIterator(IndexedSeq(Seq(), Seq("a", "b"), Seq()), IndexedSeq("prop1", "prop2", "prop3"))
    it2.toSeq mustBe Seq(
      Map("prop2" -> "a"),
      Map("prop2" -> "b")
    )
  }

  it should "produce one empty assignment when all inputs are empty" in {
    val it = CrossProductIterator(IndexedSeq(Seq(), Seq(), Seq()), IndexedSeq("prop1", "prop2", "prop3"))
    it.toSeq mustBe Seq(Map())
  }
}
