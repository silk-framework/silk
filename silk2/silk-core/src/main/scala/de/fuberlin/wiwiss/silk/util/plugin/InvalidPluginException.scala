package de.fuberlin.wiwiss.silk.util.plugin

/**
 * Thrown if a plugin is invalid.
 */
class InvalidPluginException(e: String, cause: Throwable) extends Exception(e) {
  def this(e: String) = this (e, null)
}