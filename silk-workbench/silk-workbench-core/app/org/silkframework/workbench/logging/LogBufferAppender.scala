package org.silkframework.workbench.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxyUtil}
import ch.qos.logback.core.UnsynchronizedAppenderBase

/**
  * Logback appender that captures lines into a [[LogRingBuffer]].
  *
  * Renders every event to plain strings right away and keeps nothing else, since a retained event would pin the
  * argument objects of the logging call for as long as the line stays in the ring.
  * Runs on the logging hot path of every thread, so it does bounded work only. Logback reports a failing append
  * through its status manager instead of failing the logging call.
  */
class LogBufferAppender(buffer: LogRingBuffer, config: LogBufferConfig) extends UnsynchronizedAppenderBase[ILoggingEvent] {

  private val threshold = Level.toLevel(config.level, Level.INFO)

  private val excludedPrefixes = config.excludedLoggers.map(_ + ".")

  setName(LogBufferAppender.name)

  override protected def append(event: ILoggingEvent): Unit = {
    if (event.getLevel.isGreaterOrEqual(threshold) && !isExcluded(event.getLoggerName)) {
      buffer.add(
        timestamp = event.getTimeStamp,
        level = event.getLevel.toString,
        logger = event.getLoggerName,
        thread = Option(event.getThreadName),
        message = truncate(Option(event.getFormattedMessage).getOrElse("")),
        throwable = Option(event.getThrowableProxy).map(proxy => truncate(ThrowableProxyUtil.asString(proxy)))
      )
    }
  }

  /** Excluding 'audit' also excludes 'audit.graph', matching how logger names nest. Allocation free, since it runs per event. */
  private def isExcluded(loggerName: String): Boolean = {
    config.excludedLoggers.contains(loggerName) || excludedPrefixes.exists(loggerName.startsWith)
  }

  private def truncate(text: String): String = {
    if (text.length <= config.maxMessageChars) {
      text
    } else {
      text.substring(0, config.maxMessageChars) + s"... [truncated ${text.length - config.maxMessageChars} chars]"
    }
  }
}

object LogBufferAppender {

  final val name = "silkLogBuffer"
}
