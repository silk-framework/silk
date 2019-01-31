package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}

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

  it should "not give any combination if one index has zero values" in {
    val it = CrossProductIterator(IndexedSeq(Seq("A", "B"), Seq(), Seq("1", "2", "3")), IndexedSeq("prop1", "prop2", "prop3"))
    it.toSeq mustBe Seq()
  }
}
