package org.silkframework.workbench.logging

import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.status.Status
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.jdk.CollectionConverters.ListHasAsScala

class LogBufferAppenderTest extends AnyFlatSpec with Matchers {

  behavior of "LogBufferAppender"

  private val all: LogLine => Boolean = _ => true

  private def config(level: String = "INFO", excluded: Seq[String] = Seq.empty, maxChars: Int = 4096): LogBufferConfig = {
    LogBufferConfig(enabled = true, capacity = 100, level = level, maxMessageChars = maxChars, excludedLoggers = excluded)
  }

  /** A private logger context, so the global logging configuration stays untouched. */
  private def newContext(): LoggerContext = {
    val context = new LoggerContext()
    context.setMDCAdapter(new LogbackMDCAdapter())
    context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.TRACE)
    context
  }

  private def attach(context: LoggerContext, buffer: LogRingBuffer, config: LogBufferConfig): LogBufferAppender = {
    val appender = new LogBufferAppender(buffer, config)
    appender.setContext(context)
    appender.start()
    context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(appender)
    appender
  }

  it should "capture lines at or above the threshold only" in {
    val context = newContext()
    val buffer = new LogRingBuffer(100)
    attach(context, buffer, config(level = "WARN"))
    val logger = context.getLogger("org.silkframework.test")
    logger.info("info")
    logger.warn("warn")
    logger.error("error")
    val lines = buffer.last(10, all).lines
    lines.map(_.message) mustBe Seq("warn", "error")
    lines.map(_.level) mustBe Seq("WARN", "ERROR")
    lines.head.logger mustBe "org.silkframework.test"
    lines.head.thread mustBe Some(Thread.currentThread().getName)
    lines.head.throwable mustBe None
  }

  it should "never capture excluded loggers and their children" in {
    val context = newContext()
    val buffer = new LogRingBuffer(100)
    attach(context, buffer, config(excluded = Seq("audit")))
    context.getLogger("audit").info("secret")
    context.getLogger("audit.graph").info("secret")
    context.getLogger("auditor").info("visible")
    buffer.last(10, all).lines.map(_.message) mustBe Seq("visible")
  }

  it should "render the exception with its causes" in {
    val context = newContext()
    val buffer = new LogRingBuffer(100)
    attach(context, buffer, config())
    context.getLogger("test").error("failed", new RuntimeException("wrapper", new IllegalStateException("root cause")))
    val line = buffer.last(1, all).lines.head
    line.message mustBe "failed"
    // Rendered with the platform line separator
    val throwable = line.throwable.get
    throwable must startWith("java.lang.RuntimeException: wrapper")
    throwable must include("\tat ")
    throwable must include("Caused by: java.lang.IllegalStateException: root cause")
  }

  it should "truncate long messages and stack traces" in {
    val context = newContext()
    val buffer = new LogRingBuffer(100)
    attach(context, buffer, config(maxChars = 10))
    context.getLogger("test").error("a message that is too long", new RuntimeException("wrapper"))
    val line = buffer.last(1, all).lines.head
    line.message mustBe "a message ... [truncated 16 chars]"
    // The rendered stack trace is cut to the same ten characters
    val throwable = line.throwable.get
    throwable must startWith("java.lang.... [truncated ")
    throwable must endWith(" chars]")
  }

  it should "never let a logging call fail" in {
    val context = newContext()
    val failing = new LogRingBuffer(100) {
      override def add(timestamp: Long, level: String, logger: String, thread: Option[String], message: String,
                       throwable: Option[String]): Long = {
        throw new RuntimeException("buffer failure")
      }
    }
    attach(context, failing, config())
    noException must be thrownBy context.getLogger("test").info("first")
    noException must be thrownBy context.getLogger("test").info("second")
    context.getStatusManager.getCopyOfStatusList.asScala.count(_.getLevel == Status.ERROR) mustBe 1
  }
}
