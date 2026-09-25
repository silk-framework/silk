package org.silkframework.workbench.logging

import ch.qos.logback.classic.spi.LoggerContextListener
import ch.qos.logback.classic.{Level, Logger, LoggerContext}
import org.slf4j.LoggerFactory
import play.api.inject.{ApplicationLifecycle, SimpleModule, bind}

import java.net.InetAddress
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.{Random, Try}

/**
  * Keeps the [[LogBufferAppender]] attached to the Logback root logger.
  *
  * Attaching once is not enough: an external logback.xml, a reloaded configuration or a dev mode restart each call
  * LoggerContext.reset(), which removes every appender. The reset keeps reset resistant listeners and notifies them,
  * which is used to put the appender back.
  *
  * @param config        The buffer settings. Changes need a restart.
  * @param loggerContext The logger context to attach to. Only resolved while enabled.
  */
class LogBuffer(val config: LogBufferConfig, loggerContext: => LoggerContext) {

  private val log = java.util.logging.Logger.getLogger(getClass.getName)

  private lazy val context = loggerContext

  private val buffer: Option[LogRingBuffer] = if (config.enabled) Some(new LogRingBuffer(config.capacity)) else None

  private val appender: Option[LogBufferAppender] = buffer.map(new LogBufferAppender(_, config))

  /** Identifies the sequence space of this buffer: the host name with a suffix that differs on every start. */
  lazy val instanceId: String = s"${LogBuffer.hostName}-${Random.nextLong().toHexString}"

  /** The store to read lines from, if capturing is enabled. */
  def store: Option[LogStore] = buffer

  /** True, if the appender of this buffer is the one currently installed on the root logger. */
  def isAttached: Boolean = appender.exists(a => rootLogger.getAppender(LogBufferAppender.name) eq a)

  /** Application loggers configured with additivity=false, whose output never reaches the root logger. */
  def nonAdditiveLoggers: Seq[String] = if (isAttached) detectNonAdditiveLoggers() else Seq.empty

  /** Attaches the appender and keeps it attached across resets. Does nothing while disabled. */
  def start(): Unit = {
    if (buffer.isDefined) {
      // Listener first, so a reset in between cannot leave the appender detached
      context.addListener(Reattach)
      attach()
      log.info(s"Log buffer enabled: retaining up to ${config.capacity} lines of at most ${config.maxMessageChars} " +
        s"characters, at level ${config.level} or above")
    }
  }

  /** Detaches the appender and stops re-attaching it. Does nothing while disabled. */
  def stop(): Unit = synchronized {
    for (a <- appender) {
      context.removeListener(Reattach)
      rootLogger.detachAppender(a)
    }
  }

  /** Idempotent, since a reset may race with a fresh attach. Replaces a foreign appender of the same name. */
  private def attach(): Unit = synchronized {
    for (a <- appender) {
      val root = rootLogger
      if (root.getAppender(LogBufferAppender.name) ne a) {
        root.detachAppender(LogBufferAppender.name)
        a.setContext(context)
        a.start()
        root.addAppender(a)
      }
    }
  }

  private def rootLogger: Logger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

  /**
    * Finds application loggers whose output never reaches the root logger and therefore not the buffer.
    * Attaching to them as well would double-capture additive configurations, so the condition is only reported.
    * Computed on demand, since a reset makes every logger additive before the new configuration is applied.
    */
  private def detectNonAdditiveLoggers(): Seq[String] = {
    context.getLoggerList.asScala.iterator
      .filter(!_.isAdditive)
      .map(_.getName)
      .filter(name => LogBuffer.expectedLoggerPrefixes.exists(prefix => name == prefix || name.startsWith(prefix + ".")))
      .toSeq
  }

  private object Reattach extends LoggerContextListener {
    override def isResetResistant: Boolean = true

    override def onReset(context: LoggerContext): Unit = {
      // The reset just removed every appender, including ours
      attach()
    }

    override def onStop(context: LoggerContext): Unit = {}

    override def onStart(context: LoggerContext): Unit = {}

    override def onLevelChange(logger: Logger, level: Level): Unit = {}
  }
}

object LogBuffer {

  /** The container or host name, or the product name if neither is known. The variable is preferred, since the lookup can stall on reverse DNS. */
  lazy val hostName: String = {
    sys.env.get("HOSTNAME").filter(_.nonEmpty).orElse(Try(InetAddress.getLocalHost.getHostName).toOption).getOrElse("dataintegration")
  }

  /** Logger names whose output an administrator expects in the buffer. */
  private val expectedLoggerPrefixes = Seq("org.silkframework", "com.eccenca", "controllers", "oauth")

  /** The Logback context SLF4J is bound to. */
  def loggerContext: LoggerContext = {
    LoggerFactory.getILoggerFactory match {
      case context: LoggerContext => context
      case other => throw new IllegalStateException(s"The log buffer requires Logback, but the logging backend is ${other.getClass.getName}")
    }
  }
}

/** The log buffer of the running application. Created at startup so that startup lines are captured as well. */
@Singleton
class LogBufferService @Inject()(lifecycle: ApplicationLifecycle) extends LogBuffer(LogBufferConfig(), LogBuffer.loggerContext) {
  start()
  lifecycle.addStopHook(() => Future.successful(stop()))
}

/** Installs the log buffer. Enabled via reference.conf. */
class LogBufferModule extends SimpleModule(bind[LogBuffer].to[LogBufferService].eagerly())
