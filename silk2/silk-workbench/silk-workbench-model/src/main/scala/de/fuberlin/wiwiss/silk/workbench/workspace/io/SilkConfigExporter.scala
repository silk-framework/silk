package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object SilkConfigExporter
{
  def build() : Configuration =
  {
    val project = User().project
    val linkSpec = User().linkingTask.linkSpec

    val sources = linkSpec.datasets.map(ds => project.sourceModule.tasks.find(_.name == ds.sourceId).get.source)

    Configuration(project.config.prefixes, sources, None, linkSpec :: Nil, Nil)
  }
}