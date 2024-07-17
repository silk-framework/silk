package org.silkframework.runtime.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

/**
 * Provider of a Registry of Micrometer-based metrics.
 *
 * The `MeterRegistry` of Micrometer is used for creating and holding meters.
 *
 * For each monitoring system, there should be a single registry.
 */
sealed trait MeterRegistryProvider {
  def meterRegistry: MeterRegistry
}

/**
 * Provider of a Micrometer-based meter registry for Prometheus as a monitoring system.
 */
object PrometheusRegistryProvider extends MeterRegistryProvider {
  override val meterRegistry: MeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
