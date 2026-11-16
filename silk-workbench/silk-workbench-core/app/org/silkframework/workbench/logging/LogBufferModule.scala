package org.silkframework.workbench.logging

import play.api.inject.{ApplicationLifecycle, SimpleModule, bind}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

/** Installs the log buffer. Enabled via reference.conf. */
class LogBufferModule extends SimpleModule(bind[LogBuffer].to[LogBufferService].eagerly())

/** The log buffer of the running application. Created at startup so that startup lines are captured as well. */
@Singleton
class LogBufferService @Inject()(lifecycle: ApplicationLifecycle) extends LogBuffer(LogBufferConfig(), LogBuffer.loggerContext) {
  start()
  lifecycle.addStopHook(() => Future.successful(stop()))
}
