package org.silkframework.runtime.metrics

import io.micrometer.core.instrument.MeterRegistry
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
  import scala.jdk.CollectionConverters._

  lazy val meterRegistry: MeterRegistry = {
    val registry: PrometheusMeterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val tags: List[String] = Try {
      DefaultConfig.instance.apply().getStringList("metrics.tags")
    } match {
      case Success(pairsOfTags) if !pairsOfTags.isEmpty && pairsOfTags.size() % 2 == 0 => pairsOfTags.asScala.toList
      case _ => List("name", "cmem")
    }
    registry.config().commonTags(tags:_*)
    registry
  }
}

private object NoopRegistryProvider {
  lazy val meterRegistry: MeterRegistry = new NoopMeterRegistry()
}
