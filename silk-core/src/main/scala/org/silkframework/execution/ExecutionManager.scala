package org.silkframework.execution

import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.PluginType

/**
  * Plugin to manage the current execution.
  */
@PluginType
trait ExecutionManager extends AnyPlugin {

  /**
    * Returns the current execution. Usually this is a cached instance.
    */
  def current(): ExecutionType

}
