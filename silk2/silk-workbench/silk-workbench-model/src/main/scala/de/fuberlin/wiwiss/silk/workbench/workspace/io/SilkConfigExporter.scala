package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.config.LinkingConfig
import de.fuberlin.wiwiss.silk.workbench.workspace.User

/**
 * Builds a Silk configuration from the current Linking Task.
 */
object SilkConfigExporter {
  def build(): LinkingConfig = {
    val project = User().project
    val linkSpec = User().linkingTask.linkSpec

    LinkingConfig(
      prefixes = project.config.prefixes,
      runtime = project.linkingModule.config.runtime,
      sources = linkSpec.datasets.map(ds => project.sourceModule.tasks.find(_.name == ds.sourceId).get.source),
      linkSpecs = linkSpec :: Nil,
      outputs = Nil
    )
  }
}