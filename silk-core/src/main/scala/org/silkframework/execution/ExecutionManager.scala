package org.silkframework.execution

import org.silkframework.runtime.plugin.AnyPlugin

trait ExecutionManager extends AnyPlugin {

  def current(): ExecutionType

}