package org.silkframework.config

import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}

/**
  * A custom task provided by a plugin.
  */
trait CustomTaskPlugin extends AnyPlugin

object CustomTaskPlugin extends PluginFactory[CustomTaskPlugin]
