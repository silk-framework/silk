package org.silkframework.runtime.metrics

import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}

/**
 * Provider of a Registry of Micrometer-based metrics.
 *
 * The `MeterRegistry` of Micrometer is used for creating and holding meters.
 *
 * For each monitoring system, there should be a single registry. Here, we only provide a single Prometheus registry.
 */
object MeterRegistryProvider {
  // Registry of Micrometer-based metrics, with Prometheus as a specific implementation.
  val meterRegistry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
