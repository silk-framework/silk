package org.silkframework.config

import com.typesafe.config.{ConfigException, ConfigOrigin}

/**
 * Thrown if the configuration is invalid.
 *
 * @param configKey The configuration key that is invalid, e.g., ''config.variables''.
 * @param details Error description
 * @param cause Optional cause
 */
class InvalidConfigException(key: String, origin: ConfigOrigin, details: String, cause: Option[Throwable] = None)
  extends ConfigException(s"The configuration is invalid at key '$key': $details ($origin)", cause.orNull)
