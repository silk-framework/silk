package org.silkframework.runtime.metrics

import org.silkframework.config.DefaultConfig

import scala.util.{Success, Try}

object MetricsConfig {
  lazy val prefix: String = Try {
    DefaultConfig.instance.apply().getString("metrics.prefix")
  } match {
    case Success(prefix) if prefix.nonEmpty => prefix
    case _ => "cmem"
  }
}
