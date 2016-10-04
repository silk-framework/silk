package org.silkframework.config

import java.io.File

import com.google.inject.ImplementedBy
import com.google.inject.name.Named
import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

/**
  * Holds the configuration properties
  */
@ImplementedBy(classOf[DefaultConfig])
trait Config {
  /** Returns an instance of the current [[TypesafeConfig]] */
  def apply(): TypesafeConfig

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  def refresh(): Unit
}

@Named("default")
class DefaultConfig extends Config {

  // Overwrite default logging pattern for java.util.logging
  if (System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %3$s%n%4$s: %5$s%6$s%n")
  }

  private var config = this.synchronized {
    init()
  }

  private def init(): TypesafeConfig = {
    ConfigFactory.invalidateCaches()
    var fullConfig = ConfigFactory.load()
    // Check if we are running as part of the eccenca Linked Data Suite
    if (fullConfig.hasPath("elds.home")) {
      val eldsHome = fullConfig.getString("elds.home")
      val eldsConfig = ConfigFactory.parseFile(new File(eldsHome + "/etc/dataintegration/dataintegration.conf"))
      fullConfig = eldsConfig.withFallback(fullConfig)
    }
    // Check if we are running as part of the Play Framework
    val playConfig1 = new File(System.getProperty("user.home") + "/conf/reference.conf")
    val playConfig2 = new File(System.getProperty("user.home") + "/conf/application.conf")
    if (playConfig1.exists()) {
      fullConfig = ConfigFactory.parseFile(playConfig1).withFallback(fullConfig)
    }
    if (playConfig2.exists()) {
      fullConfig = ConfigFactory.parseFile(playConfig2).withFallback(fullConfig)
    }
    fullConfig.resolve()
  }

  def apply(): TypesafeConfig = {
    this.synchronized {
      config
    }
  }

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  override def refresh(): Unit = {
    this.synchronized {
      config = init()
    }
  }
}

object DefaultConfig {
  // This default initialization needed for usages that don't involve dependency injection
  lazy val instance = new DefaultConfig()
}