package org.silkframework.runtime.plugin

import org.silkframework.runtime.validation.ValidationException

/**
  * Thrown if a plugin cannot be created because of invalid plugin parameter values.
  */
case class InvalidPluginParameterValueException(msg: String, cause: Throwable) extends ValidationException(msg, cause) {
  def this(msg: String) = this (msg, null)
}
