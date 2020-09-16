package org.silkframework.util

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.silkframework.config.DefaultConfig

/**
  * Trait to be mixed in to modify config parameters during the run of a suite.
  * If mixed in with other traits that implement BeforeAndAfterAll, the order of mixin
  * is very important if the mixed in trait depends on the modified config. The ConfigTestTrait
  * should then be mixed in first (left) of the other trait.
  */
trait ConfigTestTrait extends BeforeAndAfterAll { this: Suite =>
  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  def propertyMap: Map[String, Option[String]]
  private var backupParameters: Iterable[(String, Option[String])] = Iterable()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    backupParameters = ConfigTestTrait.updateAndBackupParameters(propertyMap)
  }

  override protected def afterAll(): Unit = {
    ConfigTestTrait.updateAndBackupParameters(backupParameters)
    super.afterAll()
  }

}

object ConfigTestTrait {
  /** Updates the provided parameters and returns an backup of the old parameter values. */
  def updateAndBackupParameters(propertyMap: Iterable[(String, Option[String])]): Iterable[(String, Option[String])] = {
    val backup = for((key, newValue) <- propertyMap) yield {
      val oldValue = Option(System.getProperty(key))
      updateProperty(key, newValue)
      (key, oldValue)
    }
    DefaultConfig.instance.refresh()
    backup
  }

  /** Re-configures the system properties for the execution of the given block. */
  def withConfig[T](propertyMap: (String, Option[String])*)(block: => T): T = {
    val backupParameters = updateAndBackupParameters(propertyMap)
    DefaultConfig.instance.refresh()
    try {
      block
    } finally {
      updateAndBackupParameters(backupParameters)
    }
  }

  // Removes the property value if newValue is None, else sets it to the new value
  private def updateProperty(key: String, newValue: Option[String]): Unit = {
    newValue match {
      case Some(v) =>
        System.setProperty(key, v)
      case None =>
        System.clearProperty(key)
    }
  }
}
