package de.fuberlin.wiwiss.silk.workbench.lift.util

import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object ConfigBuilder
{
  def build() : Configuration =
  {
    val project = User().project
    val linkingTask = User().linkingTask

    Configuration(project.config.prefixes, project.sourceModule.tasks.map(_.source), None, linkingTask.linkSpec :: Nil, Nil)
  }
}