package org.silkframework.workspace.health

import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType
import play.api.libs.json.JsValue

/**
  * A health check that contributes a section to the application's health endpoint.
  * Implementations are registered as plugins and are picked up by the health endpoint automatically,
  * so optional modules can add their own section without the endpoint referencing them.
  */
@PluginType(label = "Health check contribution", description = "Contributes a section to the health endpoint.")
trait HealthCheckContribution extends AnyPlugin {

  /** The key under which this check appears in the health details JSON. */
  def key: String

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
