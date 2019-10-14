package org.silkframework.util

import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.silkframework.config.DefaultConfig
import scala.collection.JavaConverters._

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
    backupParameters = for((key, newValue) <- propertyMap) yield {
      val oldValue = Option(System.getProperty(key))
      updateProperty(key, newValue)
      (key, oldValue)
    }
    DefaultConfig.instance.refresh()
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

  override protected def afterAll(): Unit = {
    // Restore old System properties
    for((key, oldValue) <- backupParameters) {
      updateProperty(key, oldValue)
    }
    val overrides = ConfigFactory.parseMap(backupParameters.flatMap(x => x._2.map(y => (x._1, y))).toMap.asJava)
    DefaultConfig.instance.refresh(overrides)
    super.afterAll()
  }
}
