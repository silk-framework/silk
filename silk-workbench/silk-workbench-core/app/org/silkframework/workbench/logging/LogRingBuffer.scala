package org.silkframework.workbench.logging

import java.util.concurrent.atomic.{AtomicLong, AtomicReferenceArray}
import scala.collection.mutable.ArrayBuffer

/**
  * Lock-free ring of the most recent log lines.
  *
  * Sits on the logging hot path: add never blocks, allocates only the line and never throws. Everything is best effort,
  * a reader may miss a line that is being written at that moment.
  *
  * The slot of a sequence is sequence % capacity. A slot is only live if the line it holds still carries the sequence
  * being looked for, which detects overwritten lines without any bookkeeping. Memory is bounded by construction:
  * capacity lines, each capped by the appender while rendering.
  */
class LogRingBuffer(val capacity: Int) extends LogStore {
  require(capacity >= 1, s"capacity must be at least 1, was $capacity")

  private val slots = new AtomicReferenceArray[LogLine](capacity)

  /** Next sequence to be handed out, i.e. one past the newest readable sequence. */
  private val writeCursor = new AtomicLong(0)

  /** Appends a line, overwriting the oldest one once the ring is full. Returns the sequence of the line. */
  def add(timestamp: Long,
          level: String,
          logger: String,
          thread: Option[String],
          message: String,
          throwable: Option[String]): Long = {
    val sequence = writeCursor.getAndIncrement()
    slots.set(slotOf(sequence), LogLine(sequence, timestamp, level, logger, thread, message, throwable))
    sequence
  }

  override def since(sinceExclusive: Long, limit: Int, filter: LogLine => Boolean): LogPage = {
    val newest = lastSequence
    val from = math.max(sinceExclusive + 1, firstSequence)
    val matching = new ArrayBuffer[LogLine]()
    var examined = math.max(sinceExclusive, from - 1)
    var sequence = from
    while (sequence <= newest && matching.size < limit) {
      liveLineAt(sequence).filter(filter).foreach(matching += _)
      examined = sequence
      sequence += 1
    }
    LogPage(matching.toSeq, examined, truncated = sequence <= newest)
  }

  override def last(limit: Int, filter: LogLine => Boolean): LogPage = {
    val newest = lastSequence
    val oldest = firstSequence
    val matching = new ArrayBuffer[LogLine]()
    // Walks backwards, so that the limit keeps the newest lines
    var sequence = newest
    while (sequence >= oldest && matching.size < limit) {
      liveLineAt(sequence).filter(filter).foreach(matching += _)
      sequence -= 1
    }
    LogPage(matching.reverse.toSeq, newest, truncated = sequence >= oldest)
  }

  /** The ring holds exactly the last 'capacity' sequences, so this is pure arithmetic. */
  override def firstSequence: Long = math.max(0, writeCursor.get - capacity)

  override def lastSequence: Long = writeCursor.get - 1

  override def size: Int = math.max(0, lastSequence - firstSequence + 1).toInt

  /** The line stored under a sequence, or None if it has been overwritten since. */
  private def liveLineAt(sequence: Long): Option[LogLine] = {
    Option(slots.get(slotOf(sequence))).filter(_.sequence == sequence)
  }

  private def slotOf(sequence: Long): Int = java.lang.Math.floorMod(sequence, capacity.toLong).toInt
}
