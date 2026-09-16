package org.silkframework.workbench.logging

import java.util.concurrent.atomic.{AtomicLong, AtomicReferenceArray}
import scala.collection.mutable.ArrayBuffer

/**
  * Lock-free ring of the most recent log lines.
  *
  * Sits on the logging hot path: add never blocks, allocates only the line and never throws. A reader stops at a line
  * that is being written at that moment, so that the next poll picks it up.
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
    var inFlight = false
    while (sequence <= newest && matching.size < limit && !inFlight) {
      val line = slots.get(slotOf(sequence))
      inFlight = isInFlight(line, sequence)
      if (!inFlight) {
        if (line.sequence == sequence && filter(line)) matching += line
        examined = sequence
        sequence += 1
      }
    }
    LogPage(matching.toSeq, examined, truncated = sequence <= newest)
  }

  override def last(limit: Int, filter: LogLine => Boolean): LogPage = {
    val newest = lastSequence
    val oldest = firstSequence
    val matching = new ArrayBuffer[LogLine]()
    // Walks backwards, so that the limit keeps the newest lines
    var cursor = newest
    var sequence = newest
    while (sequence >= oldest && matching.size < limit) {
      val line = slots.get(slotOf(sequence))
      if (isInFlight(line, sequence)) {
        cursor = sequence - 1 // polling from here returns the line once it is stored
      } else if (line.sequence == sequence && filter(line)) {
        matching += line
      }
      sequence -= 1
    }
    LogPage(matching.reverse.toSeq, cursor, truncated = sequence >= oldest)
  }

  /** The ring holds exactly the last 'capacity' sequences, so this is pure arithmetic. */
  override def firstSequence: Long = math.max(0, writeCursor.get - capacity)

  override def lastSequence: Long = writeCursor.get - 1

  override def size: Int = math.max(0, lastSequence - firstSequence + 1).toInt

  /** True, if the sequence has been handed out, but its line is not stored yet. A newer line means it was overwritten. */
  private def isInFlight(line: LogLine, sequence: Long): Boolean = line == null || line.sequence < sequence

  private def slotOf(sequence: Long): Int = java.lang.Math.floorMod(sequence, capacity.toLong).toInt
}
