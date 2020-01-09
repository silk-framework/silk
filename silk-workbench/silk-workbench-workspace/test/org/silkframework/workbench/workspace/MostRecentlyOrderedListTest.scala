package org.silkframework.workbench.workspace

import org.scalatest.{FlatSpec, MustMatchers}

class MostRecentlyOrderedListTest extends FlatSpec with MustMatchers {
  behavior of "most recently ordered list"

  val capacity = 10

  it should "order its elements chronologically" in {
    val list = MostRecentlyOrderedList(capacity)
    for(v <- Seq("a", "b", "c", "d", "c", "b")) {
      list.add(v)
    }
    list.items() mustBe Seq("a", "d", "c", "b")
  }

  it should "keep only the N most recent items" in {
    val list = MostRecentlyOrderedList(capacity)
    val prefix = "prefix"
    var count = 0
    for(v <- 0 to capacity) {
      list.add(prefix + v)
      count += 1
    }
    count mustBe capacity + 1
    list.size() mustBe capacity
    list.items().head mustBe prefix + 1
  }
}
