package org.silkframework.runtime.metrics

import io.micrometer.core.instrument.{Meter, MeterRegistry}
import io.micrometer.core.instrument.config.NamingConvention
import io.micrometer.prometheus.{PrometheusConfig, PrometheusMeterRegistry}
import org.silkframework.config.DefaultConfig

import scala.util.{Failure, Success, Try}

/**
 * Provider of a Registry of Micrometer-based metrics.
 *
 * The `MeterRegistry` of Micrometer is used for creating and holding meters.
 *
 * For each monitoring system, there should be a single registry.
 */
object MeterRegistryProvider {
  lazy val meterRegistry: MeterRegistry = Try {
    DefaultConfig.instance.apply().getBoolean("metrics.enabled")
  } match {
    case Success(true) => PrometheusRegistryProvider.meterRegistry
    case Success(false) => NoopRegistryProvider.meterRegistry
    case Failure(_) => NoopRegistryProvider.meterRegistry
  }
}

/**
 * Provider of a Micrometer-based meter registry for Prometheus as a monitoring system.
 */
private object PrometheusRegistryProvider {
  private val prefix: String = "cmem.di"

  lazy val meterRegistry: MeterRegistry = {
    def prefixed(namingConvention: NamingConvention): NamingConvention =
      (name: String, `type`: Meter.Type, baseUnit: String) => namingConvention.name(s"$prefix.$name", `type`, baseUnit)
    val registry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val namingConvention = registry.config().namingConvention()
    registry.config().namingConvention(prefixed(namingConvention))
    registry
  }
}

private object NoopRegistryProvider {
  lazy val meterRegistry: MeterRegistry = new NoopMeterRegistry()
}
