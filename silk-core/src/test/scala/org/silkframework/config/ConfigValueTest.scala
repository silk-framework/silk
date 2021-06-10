package org.silkframework.config

import com.typesafe.config.{Config => TypesafeConfig}
import org.scalatest.{FlatSpec, Matchers}

class ConfigValueTest extends FlatSpec with Matchers {

  behavior of "ConfigValue"

  it should "cache config values" in {
    System.setProperty("testConfigValue", "value1")
    TestConfigValue.loadCounter shouldBe 0
    TestConfigValue() shouldBe "value1"
    TestConfigValue.loadCounter shouldBe 1
    TestConfigValue() shouldBe "value1"
    TestConfigValue.loadCounter shouldBe 1
  }

  it should "reload config values if the config has been refreshed in the mean time" in {
    System.setProperty("testConfigValue", "value2")

    // The cache still holds the previous value
    TestConfigValue.loadCounter shouldBe 1
    TestConfigValue() shouldBe "value1"

    // After refresh, the cache should be reloaded
    DefaultConfig.instance.refresh()
    TestConfigValue() shouldBe "value2"
    TestConfigValue.loadCounter shouldBe 2
  }

  // Simple test config value that counts the reloads
  object TestConfigValue extends ConfigValue[String] {
    var loadCounter = 0

    override protected def load(config: TypesafeConfig): String = {
      loadCounter += 1
      config.getString("testConfigValue")
    }
  }

}
