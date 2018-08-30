package org.silkframework.execution

import org.silkframework.runtime.plugin.PluginRegistry

object Execution {

  private lazy val executionManager = {
    PluginRegistry.createFromConfig[ExecutionManager]("execution.manager")
  }

  def current(): ExecutionType = {
    executionManager.current()
  }

}
