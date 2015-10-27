package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.plugin.AnyPlugin
import de.fuberlin.wiwiss.silk.workspace.Project

trait ProjectExecutor extends AnyPlugin {

  def execute(project: Project): Unit

}
