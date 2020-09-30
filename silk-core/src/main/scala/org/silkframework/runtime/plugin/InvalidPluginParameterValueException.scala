package org.silkframework.runtime.plugin

import org.silkframework.runtime.validation.ValidationException

/**
  * Thrown if a plugin cannot be created because of invalid plugin parameter values.
  */
class InvalidPluginParameterValueException(msg: String, cause: Throwable) extends ValidationException(msg) {
  def this(msg: String) = this (msg, null)
}
