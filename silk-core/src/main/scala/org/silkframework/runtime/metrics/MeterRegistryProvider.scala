package org.silkframework.runtime.metrics

import io.micrometer.core.instrument.{Meter, MeterRegistry}
import io.micrometer.core.instrument.config.NamingConvention
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
  private val prefix: String = "cmem.di"

  override val meterRegistry: MeterRegistry = {
    val registry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val prefixedNamingConvention: NamingConvention = (name: String, `type`: Meter.Type, baseUnit: String) =>
      registry.config().namingConvention.name(s"$prefix.$name", `type`, baseUnit)
    registry.config().namingConvention(prefixedNamingConvention)
    registry
  }
}
