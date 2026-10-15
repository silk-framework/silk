package org.silkframework.workbench.logging

/**
  * A captured log line.
  * Holds strings only, so that no argument objects of the original logging call stay referenced.
  *
  * @param sequence  Position in the log stream of the capturing instance. Also the polling cursor.
  * @param timestamp Epoch millis the line was logged at.
  * @param level     Level name, e.g. WARN.
  * @param logger    Name of the logger.
  * @param thread    Name of the logging thread.
  * @param message   Formatted, possibly truncated message.
  * @param throwable Rendered stack trace, if the line carried an exception.
  */
case class LogLine(sequence: Long,
                   timestamp: Long,
                   level: String,
                   logger: String,
                   thread: Option[String],
                   message: String,
                   throwable: Option[String])
