package org.silkframework.runtime.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.{PrometheusConfig, PrometheusMeterRegistry}
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
  lazy val meterRegistry: MeterRegistry = {
    def app: String = Try {
      DefaultConfig.instance.apply().getString("metrics.app")
    } match {
      case Success(app) if app.nonEmpty => app
      case _ => "silk"
    }

    val registry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    registry.config().commonTags("app", app)
    registry
  }
}

private object NoopRegistryProvider {
  lazy val meterRegistry: MeterRegistry = new NoopMeterRegistry()
}
