package org.silkframework.runtime.plugin.types

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}

class KeyValuePairsTest extends AnyFlatSpec with Matchers {

  private implicit val pluginContext: PluginContext = TestPluginContext()

  behavior of "KeyValuePairsType"

  it should "parse key-value pairs from YAML string" in {
    val yaml = "key1: value1\nkey2: value2"
    val keyValuePairs = KeyValuePairsType.fromString(yaml)
    keyValuePairs.values shouldBe Map("key1" -> "value1", "key2" -> "value2")
  }

  it should "convert key-value pairs to YAML string" in {
    val keyValuePairs = KeyValuePairs(Map("key1" -> "value1", "key2" -> "value2"))
    val yaml = KeyValuePairsType.toString(keyValuePairs)
    yaml shouldBe "key1: value1\nkey2: value2\n"
  }

  it should "parse empty key-value pairs from an empty YAML string" in {
    val yaml = ""
    val keyValuePairs = KeyValuePairsType.fromString(yaml)
    keyValuePairs.values shouldBe Map.empty[String, String]
  }

  it should "convert empty key-value pairs to an empty YAML string" in {
    val keyValuePairs = KeyValuePairs(Map.empty[String, String])
    val yaml = KeyValuePairsType.toString(keyValuePairs)
    yaml shouldBe ""
  }

}
