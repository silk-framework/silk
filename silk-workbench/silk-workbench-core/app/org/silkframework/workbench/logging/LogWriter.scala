package org.silkframework.workbench.logging

/** Write access for captured log lines, the counterpart of [[LogReader]]. The appender writes through this. */
trait LogWriter {

  /** Appends a line and returns its sequence. Runs on the logging hot path, so it must not block. */
  def add(timestamp: Long,
          level: String,
          logger: String,
          thread: Option[String],
          message: String,
          throwable: Option[String]): Long
}
