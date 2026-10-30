package org.silkframework.workbench.logging

import java.util.concurrent.atomic.AtomicLong

/** Runs the [[LogReader]] contract against the ring, plus what is specific to the ring. */
class LogRingBufferTest extends LogReaderTest {

  override protected def newStore(capacity: Int): LogRingBuffer = new LogRingBuffer(capacity)

  behavior of "LogRingBuffer"

  it should "skip a line that is still being written" in {
    val buffer = newStore(4)
    fill(buffer, 2)
    // Reserves sequence 2 without storing its line, as a preempted writer would
    val cursor = classOf[LogRingBuffer].getDeclaredField("writeCursor")
    cursor.setAccessible(true)
    cursor.get(buffer).asInstanceOf[AtomicLong].incrementAndGet()
    add(buffer, "line 3")
    buffer.since(-1, 10, all).lines.map(_.message) mustBe Seq("line 0", "line 1", "line 3")
    buffer.last(10, all).lines.map(_.message) mustBe Seq("line 0", "line 1", "line 3")
  }

  it should "reject a capacity below one" in {
    an[IllegalArgumentException] must be thrownBy new LogRingBuffer(0)
  }
}
