package org.silkframework.config

import com.typesafe.config.{ConfigException, ConfigFactory, Config => TypesafeConfig}
import org.silkframework.config.Config._
import org.silkframework.runtime.validation.ValidationException

import java.io.File
import java.time.{Duration, Instant}
import java.util.logging.Logger
import javax.inject.Named
import scala.language.implicitConversions

/**
  * Holds the configuration properties
  */
trait Config {
  /** Returns an instance of the current [[TypesafeConfig]] */
  def apply(): TypesafeConfig

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  def refresh(): Unit

  /** Timestamp when the config has been loaded the last time. Updated on each refresh. */
  def timestamp: Instant
}

object Config{
  final val ELDS_HOME_ENV: String = "ELDS_HOME"
  final val ELDS_HOME_CONF: String = "elds.home"
  final val USER_HOME_CONF: String = "user.home"
  final val DATAINTEGRATION_PATH: String = "/etc/dataintegration"
  final val DATAINTEGRATION_CONFIG_DIR: String = DATAINTEGRATION_PATH + "/conf"
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

  private var currentTimestamp = Instant.now()

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
          val configFile = new File(eldsHome, dataintegrationConfigPath)
          if (!configFile.exists) {
            val msg = new StringBuilder
            msg ++= s"Configuration file not found at: ${configFile.getAbsolutePath}.\n"
            msg ++= s"Falling back on default reference.conf file.\n"
            msg ++= "Possible fix: Map a volume with the config file to this location.\n"
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
      currentTimestamp = Instant.now()
      fullConfig.resolve()
    }
  }

  def apply(): TypesafeConfig = {
    this.synchronized {
      config
    }
  }

  def extendedTypesafeConfig(): ExtendedTypesafeConfig = {
    ExtendedTypesafeConfig(apply())
  }

  override def timestamp: Instant = synchronized {
    currentTimestamp
  }

  /**
    * Loads the config for a particular class.
    */
  def forClass(clazz: Class[_], mustExist: Boolean = true): TypesafeConfig = {
    val config = apply()
    val className = clazz.getName.stripSuffix("$")
    if(config.hasPath(className)) {
      config.getConfig(className)
    } else if(!mustExist) {
      ConfigFactory.empty()
    } else {
      throw new ValidationException(s"Required configuration not found at $className.")
    }
  }

  /** Refreshes the Config instance, e.g. load from changed config file or newly set property values. */
  override def refresh(): Unit = {
    this.synchronized {
      config = init()
    }
  }
}

case class ExtendedTypesafeConfig(typesafeConfig: TypesafeConfig) {
  /** Fetches the typed value of the given key, or returns the fallback value if it does not exist. This still throws an
    * exception if the config value is not of the correct type.
    *
    * @param key           The config key.
    * @param expectedType  The label of the expected type.
    * @param fallbackValue The fallback value if no config value exists.
    * @param valueFetcher  The function that is called when the value exists.
    */
  private def getTypedValueOrElse[T](key: String, expectedType: String, fallbackValue: T, valueFetcher: String => T): T = {
    if (typesafeConfig.hasPath(key)) {
      try {
        valueFetcher(key)
      } catch {
        case _: ConfigException.WrongType =>
          throw new RuntimeException(s"Config parameter '$key' could not be read, because its value does not seem to be of type $expectedType." +
            s"Actual type: ${typesafeConfig.getAnyRef(key).getClass.getSimpleName}")
      }
    } else {
      fallbackValue
    }
  }


  /** Fetch the Boolean value of the given key, or the fallback value if it does not exist. This still throws an
    * exception if the config value is not a boolean. */
  def getBooleanOrElse(key: String, fallbackValue: Boolean): Boolean = {
    getTypedValueOrElse(key, "Boolean", fallbackValue, (key: String) => typesafeConfig.getBoolean(key))
  }

  /** Fetch the Int value of the given key, or the fallback value if it does not exist. This still throws an
    * exception if the config value is not an int. */
  def getIntOrElse(key: String, fallbackValue: Int): Int = {
    getTypedValueOrElse(key, "Integer", fallbackValue, (key: String) => typesafeConfig.getInt(key))
  }

  /** Fetch the Int value of the given key, or the fallback value if it does not exist. This still throws an
    * exception if the config value is not a duration. */
  def getDurationOrElse(key: String, fallbackValue: Duration): Duration = {
    getTypedValueOrElse(key, "Duration", fallbackValue, (key: String) => typesafeConfig.getDuration(key))
  }

  /** Fetch the Long value of the given key, or the fallback value if it does not exist. This still throws an
    * exception if the config value is not an Long. */
  def getLongOrElse(key: String, fallbackValue: Long): Long = {
    getTypedValueOrElse(key, "Long", fallbackValue, (key: String) => typesafeConfig.getLong(key))
  }

  /** Fetch the Long value of the given key, or the fallback value if it does not exist. This still throws an
    * exception if the config value is not an Long. */
  def getStringOrElse(key: String, fallbackValue: String): String = {
    getTypedValueOrElse(key, "String", fallbackValue, (key: String) => typesafeConfig.getString(key))
  }
}

object ExtendedTypesafeConfig {
  implicit def extendTypesafeConfig(typesafeConfig: TypesafeConfig): ExtendedTypesafeConfig = ExtendedTypesafeConfig(typesafeConfig)
  implicit def extendedToTypesafeConfig(extendedTypesafeConfig: ExtendedTypesafeConfig): TypesafeConfig = extendedTypesafeConfig.typesafeConfig
}

object DefaultConfig {
  // This default initialization needed for usages that don't involve dependency injection
  lazy val instance = new DefaultConfig()
}