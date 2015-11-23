package de.fuberlin.wiwiss.silk.workspace.modules

import de.fuberlin.wiwiss.silk.runtime.activity.Activity
import de.fuberlin.wiwiss.silk.runtime.plugin.AnyPlugin
import de.fuberlin.wiwiss.silk.workspace.Project

trait ProjectExecutor extends AnyPlugin with (Project => Activity[Unit]) {

  def apply(project: Project): Activity[Unit]

}
