package org.silkframework.config

import com.typesafe.config.{Config => TypesafeConfig}

import java.time.Instant
import scala.reflect.ClassTag

/**
  * Holds a configuration value.
  * The value is cached and can be retrieved efficiently.
  * Reloads the value if the configuration has been refreshed in the meantime.
  */
abstract class ConfigValue[T]() {

  private var value: Option[T] = None

  private var timestamp: Instant = Instant.MIN

  /**
    * Retrieve the cached config value.
    */
  final def apply(): T = synchronized {
    val root = DefaultConfig.instance
    if(value.isEmpty || timestamp.isBefore(root.timestamp)) {
      value = Some(load(config))
      timestamp = root.timestamp
    }
    value.get
  }

  /**
    * Retrieves the configuration.
    */
  protected def config: TypesafeConfig = DefaultConfig.instance()

  /**
    * Loads the value from the configuration.
    * To be implemented in subclasses.
    */
  protected def load(config: TypesafeConfig): T

}

/**
  * Holds a cache configuration value for a class.
  * The configuration will be loaded from the full class name.
  *
  * @tparam CLASS The class that holds the configuration.
  * @tparam T The configuration value type.
  */
abstract class ClassConfigValue[T: ClassTag]() extends ConfigValue[T] {

  /**
    * Retrieves the configuration for the given class.
    */
  protected override def config: TypesafeConfig = {
    DefaultConfig.instance.forClass(implicitly[ClassTag[T]].runtimeClass)
  }

}
