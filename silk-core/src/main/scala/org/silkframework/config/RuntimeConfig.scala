package org.silkframework.config

import java.util.logging.Level

trait RuntimeConfig {

  def blocking: Blocking

  def logLevel: Level

  def partitionSize: Int

  def reloadCache: Boolean

}

object RuntimeConfig {

  def apply(blocking: Blocking = Blocking(),
            logLevel: Level = Level.INFO,
            partitionSize: Int = 1000,
            reloadCache: Boolean = true): RuntimeConfig = {
    RuntimeConfigImpl(blocking, logLevel, partitionSize, reloadCache)
  }

  private case class RuntimeConfigImpl(blocking: Blocking, logLevel: Level, partitionSize: Int, reloadCache: Boolean) extends RuntimeConfig

}
