package org.silkframework.workbench.logging

import java.util.concurrent.atomic.{AtomicLong, AtomicReferenceArray}
import scala.collection.mutable.ArrayBuffer

/**
  * Lock-free ring of the most recent log lines.
  *
  * Sits on the logging hot path: add never blocks, allocates only the line and never throws. A reader skips a line
  * that is being written at that moment, so a poll can miss it. For a diagnostic buffer that is a good trade.
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
    val matching = new ArrayBuffer[LogLine]()
    var sequence = math.max(sinceExclusive + 1, firstSequence)
    while (sequence <= newest && matching.size < limit) {
      val line = liveLine(sequence)
      if (line != null && filter(line)) matching += line
      sequence += 1
    }
    LogPage(matching.toSeq, sequence - 1, truncated = sequence <= newest)
  }

  override def last(limit: Int, filter: LogLine => Boolean): LogPage = {
    val newest = lastSequence
    val oldest = firstSequence
    val matching = new ArrayBuffer[LogLine]()
    // Walks backwards, so that the limit keeps the newest lines
    var sequence = newest
    while (sequence >= oldest && matching.size < limit) {
      val line = liveLine(sequence)
      if (line != null && filter(line)) matching += line
      sequence -= 1
    }
    LogPage(matching.reverse.toSeq, newest, truncated = sequence >= oldest)
  }

  /** The ring holds exactly the last 'capacity' sequences, so this is pure arithmetic. */
  override def firstSequence: Long = math.max(0, writeCursor.get - capacity)

  override def lastSequence: Long = writeCursor.get - 1

  override def size: Int = math.max(0, lastSequence - firstSequence + 1).toInt

  /** The line of a sequence, or null if it is overwritten or not stored yet. */
  private def liveLine(sequence: Long): LogLine = {
    val line = slots.get(slotOf(sequence))
    if (line != null && line.sequence == sequence) line else null
  }

  private def slotOf(sequence: Long): Int = java.lang.Math.floorMod(sequence, capacity.toLong).toInt
}
