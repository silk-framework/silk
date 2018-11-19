package org.silkframework.workspace.activity

import org.silkframework.runtime.plugin.AnyPlugin

trait WorkspaceActivityFactory extends AnyPlugin {

  /** True, if there is always exactly one instance of this activity */
  def isSingleton: Boolean = true

}
