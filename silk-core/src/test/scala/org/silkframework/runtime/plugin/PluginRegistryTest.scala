package org.silkframework.runtime.plugin

import org.scalatest.{FlatSpec, MustMatchers}

class PluginRegistryTest extends FlatSpec with MustMatchers {

  behavior of "PluginRegistry"

  it should "return plugin descriptions for registered classes" in  {
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe None
    PluginRegistry.registerPlugin(classOf[TestPlugin])
    PluginRegistry.pluginDescription(classOf[TestPlugin]) mustBe ('defined)
  }
}


