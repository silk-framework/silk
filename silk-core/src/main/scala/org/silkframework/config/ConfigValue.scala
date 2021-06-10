package org.silkframework.config

import com.typesafe.config.{Config => TypesafeConfig}

import java.time.Instant

/**
  * Holds a configuration value.
  * The value is cached and can be retrieved efficiently.
  * Reloads the value if the configuration has been refreshed in the mean time.
  */
abstract class ConfigValue[T]() {

  private var value: Option[T] = None

  private var timestamp: Instant = Instant.MIN

  def apply(): T = synchronized {
    val config = DefaultConfig.instance
    if(value.isEmpty || timestamp.isBefore(config.timestamp)) {
      value = Some(load(config()))
      timestamp = config.timestamp
    }
    value.get
  }

  protected def load(config: TypesafeConfig): T

}
