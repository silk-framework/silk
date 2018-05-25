package org.silkframework.config

import java.io.File
import java.util.logging.Logger
import javax.inject.Named
import Config._

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}

/**
  * Holds the configuration properties
  */
trait Config {
  /** Returns an instance of the current [[TypesafeConfig]] */
  def apply(): TypesafeConfig

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  def refresh(): Unit
}

object Config{
  final val ELDS_HOME_ENV: String = "ELDS_HOME"
  final val ELDS_HOME_CONF: String = "elds.home"
  final val USER_HOME_CONF: String = "user.home"
  final val DATAINTEGRATION_PATH: String = "/etc/dataintegration"
  final val DATAINTEGRATION_CONF: String = "/conf/dataintegration.conf"
  final val REFERENCE_CONF: String = "/conf/reference.conf"
  final val APPLICATION_CONF: String = "/conf/application.conf"
}

@Named("default")
class DefaultConfig private() extends Config {
  // Overwrite default logging pattern for java.util.logging
  if (System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %3$s%n%4$s: %5$s%6$s%n")
  }

  private lazy val log = Logger.getLogger(this.getClass.getName)

  private var config = this.synchronized {init()}

  /**
    * Will check and return if ELDS_HOME was defined either as environment variable or in the dataintegration config.
    */
  lazy val eldsHomeDir: Option[File] ={
    ConfigFactory.invalidateCaches()
    val conf = ConfigFactory.load()
    Option(if(conf.hasPath(ELDS_HOME_CONF)) conf.getString(ELDS_HOME_CONF) else System.getenv(ELDS_HOME_ENV))
      .map(p => new File(p))
  }

  private def init(): TypesafeConfig = {
    this.synchronized {
      ConfigFactory.invalidateCaches()
      var fullConfig = ConfigFactory.load()
      // Check if we are running as part of the eccenca Linked Data Suite
      eldsHomeDir match {
        case Some(eldsHome) =>
          val dataintegrationConfigPath = DATAINTEGRATION_PATH + DATAINTEGRATION_CONF
          val configFile = new File(eldsHome + dataintegrationConfigPath)
          if (!configFile.exists) {
            val msg = new StringBuilder
            msg ++= s"Mandatory configuration file not found at: ${configFile.getAbsolutePath} "
            if(System.getenv(ELDS_HOME_ENV) != null)
              msg ++= "and " + System.getenv(ELDS_HOME_ENV) + dataintegrationConfigPath + ""
            msg ++= ". Possible fix: Map a volume with the config file to this location. "
            msg ++= "Otherwise set elds.home or $ELDS_HOME to point to the correct location."
            log.warning(msg.toString())
          }
          fullConfig = ConfigFactory.parseFile(configFile).withFallback(fullConfig)
        case None => Logger.getLogger(this.getClass.getName).info(
          "Variable $ELDS_HOME is not defined. If this application is not running in the ELDS context " +
            "you can ignore this warning. Otherwise please configure $ELDS_HOME or elds.home."
        )
      }

      // Check if we are running as part of the Play Framework
      val playConfig1 = new File(System.getProperty(USER_HOME_CONF) + REFERENCE_CONF)
      val playConfig2 = new File(System.getProperty(USER_HOME_CONF) + APPLICATION_CONF)
      if (playConfig1.exists()) {
        fullConfig = fullConfig.withFallback(ConfigFactory.parseFile(playConfig1))
      }
      if (playConfig2.exists()) {
        fullConfig = fullConfig.withFallback(ConfigFactory.parseFile(playConfig2))
      }
      fullConfig.resolve()
    }
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