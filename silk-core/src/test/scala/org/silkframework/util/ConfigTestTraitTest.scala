package org.silkframework.util

import com.typesafe.config.ConfigException
import org.silkframework.config.DefaultConfig
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ConfigTestTraitTest extends AnyFlatSpec with Matchers with ConfigTestTrait {
  behavior of "Config Test Trait"

  val propertyKey = "test.property.xyz"
  val propertyKeyLater = "test.property.later.xyz"
  val propertyValue = "test value"

  it should "modify the values of the DefaultConfig" in {
    val config = DefaultConfig.instance()
    config.getString(propertyKey) mustBe propertyValue
    intercept[ConfigException] {
      config.getString(propertyKeyLater)
    }
  }

  it should "modify the values by refreshing the Config" in {
    System.setProperty(propertyKeyLater, propertyValue)
    val config = DefaultConfig.instance
    config.refresh()
    config().getString(propertyKeyLater) mustBe propertyValue
  }

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = {
    Map(
      propertyKey -> Some(propertyValue)
    )
  }
}
