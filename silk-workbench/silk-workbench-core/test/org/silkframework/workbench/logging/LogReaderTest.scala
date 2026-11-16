package org.silkframework.workbench.logging

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/** Contract of [[LogReader]]. The test of an implementation mixes this in and provides the store. */
trait LogReaderTest extends AnyFlatSpec with Matchers {

  behavior of "LogReader"

  protected val all: LogLine => Boolean = _ => true

  /** A fresh, empty store of the given capacity. */
  protected def newStore(capacity: Int): LogReader with LogWriter

  protected def add(store: LogWriter, message: String, level: String = "INFO"): Long = {
    store.add(timestamp = 1L, level = level, logger = "test", thread = Some("main"), message = message, throwable = None)
  }

  protected def fill(store: LogWriter, count: Int): Unit = {
    for (i <- 0 until count) add(store, s"line $i")
  }

  it should "return the newest lines oldest first" in {
    val store = newStore(10)
    fill(store, 5)
    val page = store.last(3, all)
    page.lines.map(_.message) mustBe Seq("line 2", "line 3", "line 4")
    page.truncated mustBe true
    page.nextCursor mustBe 4
    store.last(10, all).truncated mustBe false
  }

  it should "evict the oldest lines once full" in {
    val store = newStore(3)
    fill(store, 5)
    store.capacity mustBe 3
    store.firstSequence mustBe 2
    store.lastSequence mustBe 4
    store.size mustBe 3
    store.since(-1, 10, all).lines.map(_.sequence) mustBe Seq(2, 3, 4)
  }

  it should "poll from a cursor" in {
    val store = newStore(10)
    fill(store, 3)
    val first = store.since(-1, 10, all)
    first.lines.map(_.sequence) mustBe Seq(0, 1, 2)
    first.nextCursor mustBe 2
    first.truncated mustBe false
    val nothingNew = store.since(first.nextCursor, 10, all)
    nothingNew.lines mustBe empty
    nothingNew.nextCursor mustBe 2
    add(store, "new")
    val next = store.since(first.nextCursor, 10, all)
    next.lines.map(_.message) mustBe Seq("new")
    next.nextCursor mustBe 3
  }

  it should "advance the cursor past filtered lines and stop at a truncated page" in {
    val store = newStore(10)
    add(store, "a", "INFO")
    add(store, "b", "WARN")
    add(store, "c", "INFO")
    add(store, "d", "WARN")
    add(store, "e", "WARN")
    val warnings: LogLine => Boolean = _.level == "WARN"
    val page = store.since(-1, 2, warnings)
    page.lines.map(_.message) mustBe Seq("b", "d")
    page.nextCursor mustBe 3
    page.truncated mustBe true
    val rest = store.since(page.nextCursor, 2, warnings)
    rest.lines.map(_.message) mustBe Seq("e")
    rest.nextCursor mustBe 4
    rest.truncated mustBe false
  }

  it should "start at the oldest retained line if the cursor points to evicted lines" in {
    val store = newStore(3)
    fill(store, 6)
    store.since(0, 10, all).lines.map(_.sequence) mustBe Seq(3, 4, 5)
  }

  it should "handle an empty store" in {
    val store = newStore(3)
    store.lastSequence mustBe -1
    store.firstSequence mustBe 0
    store.size mustBe 0
    store.last(5, all) mustBe LogPage(Seq.empty, -1, truncated = false)
    store.since(-1, 5, all) mustBe LogPage(Seq.empty, -1, truncated = false)
    store.since(10, 5, all).nextCursor mustBe 10
  }
}
