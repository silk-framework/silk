package org.silkframework.runtime.plugin


import org.silkframework.util.ConfigTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PluginRegistryConfigTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "PluginRegistry.Config"

  it should "load plugin blacklist" in {
    PluginRegistry.Config.blacklistedPlugins().map(_.toString) shouldBe Set("GeoLocationTypeDiscoverer", "script", "legacyId1", "legacyId2")
  }

  /** The properties that should be changed.
    * If the value is [[None]] then the property value is removed,
    * else it is set to the new value.
    */
  override def propertyMap: Map[String, Option[String]] = {
    Map(
      "pluginRegistry.plugins.EnabledPlugin.enabled" -> Some("true"), // Should do nothing as "true" is the default
      "pluginRegistry.plugins.GeoLocationTypeDiscoverer.enabled" -> Some("false"),
      "pluginRegistry.plugins.script.enabled" -> Some("false"),
      "plugin.blacklist" -> Some("legacyId1, legacyId2") // Legacy parameters should still be supported
    )
  }
}
