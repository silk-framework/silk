package org.silkframework.workspace.health

import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType
import play.api.libs.json.JsValue

import scala.util.control.NonFatal

/**
  * A health check that contributes a section to the application's health endpoint.
  * Implementations are registered as plugins and are picked up by the health endpoint automatically,
  * so optional modules can add their own section without the endpoint referencing them.
  * The plugin id is the key of the contributed section, so that it stays the same even if the check fails.
  */
@PluginType(label = "Health check contribution", description = "Contributes a section to the health endpoint.")
trait HealthCheckContribution extends AnyPlugin {

  /** Runs the health check. Should not throw, failures are reported via the result. */
  def check(): HealthCheckResult
}

/**
  * Result of a single health check.
  *
  * @param healthy Whether the checked component is up.
  * @param details Check-specific detail fields.
  */
case class HealthCheckResult(healthy: Boolean, details: JsValue)

/**
  * Matches failures that a health check reports instead of propagating.
  * Wider than [[NonFatal]] by [[LinkageError]], which a failing static initializer throws, e.g. when a Spark
  * context cannot be created. Reporting a broken component is the purpose of the endpoint, so it must not fail on it.
  */
object HealthCheckFailure {

  def unapply(ex: Throwable): Option[Throwable] = {
    ex match {
      case NonFatal(_) | _: LinkageError => Some(ex)
      case _ => None
    }
  }

  /** Message to report. Falls back to the class name, since a [[LinkageError]] often has no message. */
  def message(ex: Throwable): String = Option(ex.getMessage).getOrElse(ex.toString)
}
