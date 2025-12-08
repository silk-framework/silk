package org.silkframework.runtime.plugin.types

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}

import scala.collection.immutable.ListMap

class KeyValuePairsTest extends AnyFlatSpec with Matchers {

  private implicit val pluginContext: PluginContext = TestPluginContext()

  behavior of "KeyValuePairsType"

  it should "parse key-value pairs from YAML string" in {
    val yaml = "key2: value2\nkey1: value1"
    val keyValuePairs = KeyValuePairsType.fromString(yaml)
    // Also make sure that the order is preserved
    keyValuePairs.values.toSeq shouldBe Seq("key2" -> "value2", "key1" -> "value1")
  }

  it should "convert key-value pairs to YAML string" in {
    val keyValuePairs = KeyValuePairs(ListMap("key2" -> "value2", "key1" -> "value1"))
    val yaml = KeyValuePairsType.toString(keyValuePairs)
    yaml shouldBe "key2: value2\nkey1: value1\n"
  }

  it should "parse empty key-value pairs from an empty YAML string" in {
    val yaml = ""
    val keyValuePairs = KeyValuePairsType.fromString(yaml)
    keyValuePairs.values shouldBe ListMap.empty[String, String]
  }

  it should "convert empty key-value pairs to an empty YAML string" in {
    val yaml = KeyValuePairsType.toString(KeyValuePairs.empty)
    yaml shouldBe ""
  }

}
