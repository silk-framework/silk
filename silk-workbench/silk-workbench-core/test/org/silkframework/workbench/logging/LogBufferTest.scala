package org.silkframework.workbench.logging

import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.classic.LoggerContext
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class LogBufferTest extends AnyFlatSpec with Matchers {

  behavior of "LogBuffer"

  private val all: LogLine => Boolean = _ => true

  private val enabled = LogBufferConfig(enabled = true, capacity = 10, level = "INFO", maxMessageChars = 4096, excludedLoggers = Seq.empty)

  /** A private logger context, so the global logging configuration stays untouched. */
  private def newContext(): LoggerContext = {
    val context = new LoggerContext()
    context.setMDCAdapter(new LogbackMDCAdapter())
    context
  }

  private def rootAppender(context: LoggerContext) = {
    Option(context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getAppender(LogBufferAppender.name))
  }

  it should "capture lines logged to the root logger after start" in {
    val context = newContext()
    val logBuffer = new LogBuffer(enabled, context)
    logBuffer.start()
    logBuffer.isAttached mustBe true
    rootAppender(context) mustBe defined
    context.getLogger("org.silkframework.test").info("hello")
    logBuffer.store.get.last(10, all).lines.map(_.message) mustBe Seq("hello")
  }

  it should "re-attach after the logger context is reset" in {
    val context = newContext()
    val logBuffer = new LogBuffer(enabled, context)
    logBuffer.start()
    context.getLogger("org.silkframework.test").info("before reset")
    context.reset()
    rootAppender(context) mustBe defined
    logBuffer.isAttached mustBe true
    context.getLogger("org.silkframework.test").info("after reset")
    logBuffer.store.get.last(10, all).lines.map(_.message) mustBe Seq("before reset", "after reset")
  }

  it should "report non-additive application loggers" in {
    val context = newContext()
    context.getLogger("org.silkframework.silent").setAdditive(false)
    context.getLogger("com.other.silent").setAdditive(false)
    val logBuffer = new LogBuffer(enabled, context)
    logBuffer.start()
    logBuffer.nonAdditiveLoggers mustBe Seq("org.silkframework.silent")
  }

  it should "capture nothing while disabled" in {
    val context = newContext()
    val logBuffer = new LogBuffer(enabled.copy(enabled = false), context)
    logBuffer.start()
    logBuffer.store mustBe None
    logBuffer.isAttached mustBe false
    rootAppender(context) mustBe None
  }

  it should "detach on stop and stay detached across resets" in {
    val context = newContext()
    val logBuffer = new LogBuffer(enabled, context)
    logBuffer.start()
    logBuffer.stop()
    rootAppender(context) mustBe None
    logBuffer.isAttached mustBe false
    context.reset()
    rootAppender(context) mustBe None
  }
}
