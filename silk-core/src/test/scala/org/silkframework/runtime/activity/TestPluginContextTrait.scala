package org.silkframework.runtime.activity

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}

/**
  * Trait that provides a default plugin context for tests.
  */
trait TestPluginContextTrait extends TestUserContextTrait {

  implicit val prefixes: Prefixes = Prefixes.empty

  implicit lazy val pluginContext: PluginContext = TestPluginContext(user = userContext, prefixes = prefixes)
}
