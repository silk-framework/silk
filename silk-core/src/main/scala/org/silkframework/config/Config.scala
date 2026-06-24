package org.silkframework.config

import com.typesafe.config.{ConfigException, ConfigFactory, Config => TypesafeConfig}
import org.silkframework.runtime.validation.ValidationException

import java.time.{Duration, Instant}
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

@Named("default")
class DefaultConfig private() extends Config {

  // Overwrite default logging pattern for java.util.logging
  if (System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %3$s%n%4$s: %5$s%6$s%n")
  }

  private var config = this.synchronized {init()}

  private var currentTimestamp = Instant.now()

  private def init(): TypesafeConfig = {
    this.synchronized {
      ConfigFactory.invalidateCaches()

      // Check for external config file based on environment variables
      val configPath = sys.env.get("DATAINTEGRATION_CONFIG") match {
        case Some(configDir) => s"$configDir/dataintegration.conf"
        case None => sys.env.get("CMEM_HOME") match {
          case Some(cmemHome) => s"$cmemHome/dataintegration/config/dataintegration.conf"
          case None => sys.props.get("user.home").map(home => s"$home/.cmem/dataintegration/config/dataintegration.conf").getOrElse("")
        }
      }

      val configFile = new java.io.File(configPath)
      val fullConfig = if (configFile.exists()) {
        println(s"Loading external config from: $configPath")
        // Load with external config having highest priority
        val externalConfig = ConfigFactory.parseFile(configFile)
        ConfigFactory.systemProperties()
          .withFallback(externalConfig)
          .withFallback(ConfigFactory.load())
      } else {
        ConfigFactory.load()
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