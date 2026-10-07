package org.silkframework.workbench.logging

/**
  * Read access to captured log lines.
  * Backends, such as the in-memory ring or a file-based store, implement this.
  * Lines are addressed by a sequence that increases monotonically within one instance.
  */
trait LogStore {

  /** The most recent matching lines, oldest first. The limit keeps the newest lines. */
  def last(limit: Int, filter: LogLine => Boolean): LogPage

  /**
    * Matching lines with a sequence above the given one, oldest first, as used by a polling client.
    *
    * @param sinceExclusive Return lines with a strictly higher sequence. A negative value reads from the start.
    */
  def since(sinceExclusive: Long, limit: Int, filter: LogLine => Boolean): LogPage

  /** Oldest sequence that can still be read. */
  def firstSequence: Long

  /** Newest sequence, or -1 while the store is empty. */
  def lastSequence: Long

  /** Number of lines currently retained. */
  def size: Int

  /** Maximum number of retained lines. */
  def capacity: Int
}
