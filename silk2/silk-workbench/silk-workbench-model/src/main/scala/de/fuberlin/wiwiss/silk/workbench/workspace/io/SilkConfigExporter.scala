package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.config.SilkConfig
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object SilkConfigExporter
{
  def build() : SilkConfig =
  {
    val project = User().project
    val linkSpec = User().linkingTask.linkSpec

    val sources = linkSpec.datasets.map(ds => project.sourceModule.tasks.find(_.name == ds.sourceId).get.source)

    SilkConfig(project.config.prefixes, sources, None, linkSpec :: Nil, Nil)
  }
}