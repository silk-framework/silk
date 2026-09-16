package org.silkframework.workbench.logging

import ch.qos.logback.classic.Level
import com.typesafe.config.Config
import org.silkframework.config.ConfigValue

import scala.jdk.CollectionConverters.ListHasAsScala

/**
  * Settings of the in-memory log buffer, read from 'logging.buffer'.
  *
  * @param enabled         While false, nothing is captured and the log API answers with 503.
  * @param capacity        Maximum number of retained lines.
  * @param level           Minimum level captured, independent of the configured logger levels.
  * @param maxMessageChars Longest message retained. Also applied to the rendered stack trace.
  * @param excludedLoggers Loggers, including their children, that are never captured.
  */
case class LogBufferConfig(enabled: Boolean,
                           capacity: Int,
                           level: String,
                           maxMessageChars: Int,
                           excludedLoggers: Seq[String])

object LogBufferConfig extends ConfigValue[LogBufferConfig] {

  final val prefix = "logging.buffer"

  final val validLevels: Seq[String] = Seq("TRACE", "DEBUG", "INFO", "WARN", "ERROR")

  override protected def load(config: Config): LogBufferConfig = {
    val c = config.getConfig(prefix)
    val loaded = LogBufferConfig(
      enabled = c.getBoolean("enabled"),
      capacity = c.getInt("capacity"),
      level = c.getString("level").trim.toUpperCase,
      maxMessageChars = c.getInt("maxMessageChars"),
      excludedLoggers = c.getStringList("excludedLoggers").asScala.toSeq.map(_.trim).filter(_.nonEmpty)
    )
    validate(loaded)
    loaded
  }

  private def validate(c: LogBufferConfig): Unit = {
    if (c.capacity < 1) {
      throw new IllegalArgumentException(s"$prefix.capacity must be at least 1, was ${c.capacity}")
    }
    if (c.maxMessageChars < 1) {
      throw new IllegalArgumentException(s"$prefix.maxMessageChars must be at least 1, was ${c.maxMessageChars}")
    }
    // toLevel falls back silently, so an unknown value would otherwise capture at the wrong level
    if (Level.toLevel(c.level, null) == null || !validLevels.contains(c.level)) {
      throw new IllegalArgumentException(s"$prefix.level must be one of ${validLevels.mkString(", ")}, was '${c.level}'")
    }
  }
}
