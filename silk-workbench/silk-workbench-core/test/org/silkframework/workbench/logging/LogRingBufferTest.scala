package org.silkframework.workbench.logging

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.util.concurrent.atomic.AtomicLong

class LogRingBufferTest extends AnyFlatSpec with Matchers {

  behavior of "LogRingBuffer"

  private val all: LogLine => Boolean = _ => true

  private def add(buffer: LogRingBuffer, message: String, level: String = "INFO"): Long = {
    buffer.add(timestamp = 1L, level = level, logger = "test", thread = Some("main"), message = message, throwable = None)
  }

  private def fill(buffer: LogRingBuffer, count: Int): Unit = {
    for (i <- 0 until count) add(buffer, s"line $i")
  }

  it should "return the newest lines oldest first" in {
    val buffer = new LogRingBuffer(10)
    fill(buffer, 5)
    val page = buffer.last(3, all)
    page.lines.map(_.message) mustBe Seq("line 2", "line 3", "line 4")
    page.truncated mustBe true
    page.nextCursor mustBe 4
    buffer.last(10, all).truncated mustBe false
  }

  it should "evict the oldest lines once full" in {
    val buffer = new LogRingBuffer(3)
    fill(buffer, 5)
    buffer.firstSequence mustBe 2
    buffer.lastSequence mustBe 4
    buffer.size mustBe 3
    buffer.since(-1, 10, all).lines.map(_.sequence) mustBe Seq(2, 3, 4)
  }

  it should "poll from a cursor" in {
    val buffer = new LogRingBuffer(10)
    fill(buffer, 3)
    val first = buffer.since(-1, 10, all)
    first.lines.map(_.sequence) mustBe Seq(0, 1, 2)
    first.nextCursor mustBe 2
    first.truncated mustBe false
    val nothingNew = buffer.since(first.nextCursor, 10, all)
    nothingNew.lines mustBe empty
    nothingNew.nextCursor mustBe 2
    add(buffer, "new")
    val next = buffer.since(first.nextCursor, 10, all)
    next.lines.map(_.message) mustBe Seq("new")
    next.nextCursor mustBe 3
  }

  it should "advance the cursor past filtered lines and stop at a truncated page" in {
    val buffer = new LogRingBuffer(10)
    add(buffer, "a", "INFO")
    add(buffer, "b", "WARN")
    add(buffer, "c", "INFO")
    add(buffer, "d", "WARN")
    add(buffer, "e", "WARN")
    val warnings: LogLine => Boolean = _.level == "WARN"
    val page = buffer.since(-1, 2, warnings)
    page.lines.map(_.message) mustBe Seq("b", "d")
    page.nextCursor mustBe 3
    page.truncated mustBe true
    val rest = buffer.since(page.nextCursor, 2, warnings)
    rest.lines.map(_.message) mustBe Seq("e")
    rest.nextCursor mustBe 4
    rest.truncated mustBe false
  }

  it should "start at the oldest retained line if the cursor points to evicted lines" in {
    val buffer = new LogRingBuffer(3)
    fill(buffer, 6)
    buffer.since(0, 10, all).lines.map(_.sequence) mustBe Seq(3, 4, 5)
  }

  it should "handle an empty buffer" in {
    val buffer = new LogRingBuffer(3)
    buffer.lastSequence mustBe -1
    buffer.firstSequence mustBe 0
    buffer.size mustBe 0
    buffer.last(5, all) mustBe LogPage(Seq.empty, -1, truncated = false)
    buffer.since(-1, 5, all) mustBe LogPage(Seq.empty, -1, truncated = false)
    buffer.since(10, 5, all).nextCursor mustBe 10
  }

  it should "stop at a line that is still being written and pick it up on the next poll" in {
    val buffer = new LogRingBuffer(4)
    fill(buffer, 2)
    // Reserves sequence 2 without storing its line, as a preempted writer would
    val cursor = classOf[LogRingBuffer].getDeclaredField("writeCursor")
    cursor.setAccessible(true)
    cursor.get(buffer).asInstanceOf[AtomicLong].incrementAndGet()
    add(buffer, "line 3")
    val page = buffer.since(-1, 10, all)
    page.lines.map(_.message) mustBe Seq("line 0", "line 1")
    page.nextCursor mustBe 1
    page.truncated mustBe true
    val lastPage = buffer.last(10, all)
    lastPage.lines.map(_.message) mustBe Seq("line 0", "line 1", "line 3")
    lastPage.nextCursor mustBe 1
  }

  it should "reject a capacity below one" in {
    an[IllegalArgumentException] must be thrownBy new LogRingBuffer(0)
  }
}
