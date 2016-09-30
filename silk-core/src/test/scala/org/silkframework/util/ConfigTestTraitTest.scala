package org.silkframework.util

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.DefaultConfig

/**
  * Created on 9/30/16.
  */
class ConfigTestTraitTest extends FlatSpec with MustMatchers with ConfigTestTrait {
  behavior of "Config Test Trait"

  val propertyKey = "test.property.xyz"
  val propertyValue = "test value"

  it should "modify the values of the DefaultConfig" in {
    DefaultConfig.instance().getString(propertyKey) mustBe propertyValue
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
