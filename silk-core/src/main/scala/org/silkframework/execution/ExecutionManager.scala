package org.silkframework.execution

import org.silkframework.runtime.plugin.AnyPlugin

/**
  * Plugin to manage the current execution.
  */
trait ExecutionManager extends AnyPlugin {

  /**
    * Returns the current execution. Usually this is a cached instance.
    */
  def current(): ExecutionType

}
