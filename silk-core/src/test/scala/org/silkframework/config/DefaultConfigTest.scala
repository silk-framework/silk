package org.silkframework.config

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.util.ConfigTestTrait

class DefaultConfigTest extends FlatSpec with MustMatchers with ConfigTestTrait {
  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = Map[String, Option[String]](
    "https.port" -> Some("1234"),
    "pidfile.path" -> Some("/path/to/nowhere"),
    "play.http.parser.maxMemoryBuffer" -> Some("1MB")
  )

  it should "insert new properties into DefaultConfig" in{
    DefaultConfig.instance().getString("https.port") mustBe "1234"
  }

  it should "override properties in the dataintegration.conf" in{
    DefaultConfig.instance().getString("play.http.parser.maxMemoryBuffer") mustBe "1MB"
  }

  // needs the corresponding Java cli argument '-Ddi.test.env=value'
  ignore should "contain Java cli system properties" in{
    DefaultConfig.instance().getString("di.test.env") mustBe "value"
    DefaultConfig.instance().getString("pidfile.path") mustBe "/path/here"
  }
}
