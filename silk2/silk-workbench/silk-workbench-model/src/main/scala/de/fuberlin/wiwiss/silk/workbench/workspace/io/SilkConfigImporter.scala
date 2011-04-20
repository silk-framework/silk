package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.config.Configuration
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask

/**
 * Imports a Silk Configuration into a project.
 */
object SilkConfigImporter
{
  def apply(config : Configuration, project : Project)
  {
    //Add all prefixes
    project.config = project.config.copy(prefixes = project.config.prefixes ++ config.prefixes)

    //Add all sources
    for(source <- config.sources)
    {
      project.sourceModule.update(SourceTask(source))
    }

    //Add all linking tasks
    for(linkSpec <- config.linkSpecs)
    {
      project.linkingModule.update(LinkingTask(linkSpec.id, linkSpec))
    }
  }
}