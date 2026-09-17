package org.silkframework.workbench.logging

import ch.qos.logback.classic.util.LogbackMDCAdapter
import ch.qos.logback.classic.LoggerContext
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.util.matching.Regex

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

  it should "report non-additive application loggers, also when configured after a reset" in {
    val context = newContext()
    context.getLogger("org.silkframework.silent").setAdditive(false)
    context.getLogger("com.other.silent").setAdditive(false)
    val logBuffer = new LogBuffer(enabled, context)
    logBuffer.start()
    logBuffer.nonAdditiveLoggers mustBe Seq("org.silkframework.silent")
    // A reset makes every logger additive again, the external configuration is applied afterwards
    context.reset()
    logBuffer.nonAdditiveLoggers mustBe empty
    context.getLogger("org.silkframework.silent2").setAdditive(false)
    logBuffer.nonAdditiveLoggers mustBe Seq("org.silkframework.silent2")
  }

  it should "replace the appender of another instance instead of adopting it" in {
    val context = newContext()
    val first = new LogBuffer(enabled, context)
    first.start()
    val second = new LogBuffer(enabled, context)
    second.start()
    context.getLogger("org.silkframework.test").info("second")
    second.store.get.last(10, all).lines.map(_.message) mustBe Seq("second")
    first.store.get.last(10, all).lines mustBe empty
    second.stop()
    rootAppender(context) mustBe None
  }

  it should "identify each instance by the host name and a distinct suffix" in {
    val ids = Seq.fill(2)(new LogBuffer(enabled, newContext()).instanceId)
    ids.foreach(_ must fullyMatch regex s"${Regex.quote(LogBuffer.hostName)}-\\d+")
    ids.distinct.size mustBe 2
  }

  it should "capture nothing and not touch the logger context while disabled" in {
    val context = newContext()
    val logBuffer = new LogBuffer(enabled.copy(enabled = false), throw new IllegalStateException("must not be resolved"))
    logBuffer.start()
    logBuffer.store mustBe None
    logBuffer.isAttached mustBe false
    logBuffer.nonAdditiveLoggers mustBe empty
    logBuffer.stop()
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
