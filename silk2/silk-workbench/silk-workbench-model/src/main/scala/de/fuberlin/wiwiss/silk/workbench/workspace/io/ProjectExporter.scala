package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask

/**
 * Exports a project to a single XML file.
 */
object ProjectExporter
{
  def apply(project : Project) =
  {
    implicit val prefixes = project.config.prefixes

    <Project>
      <Config>
      { prefixes.toXML }
      </Config>
      <SourceModule>
        <Tasks>
        {
          for(task <- project.sourceModule.tasks) yield exportSourceTask(task)
        }
        </Tasks>
      </SourceModule>
      <LinkingModule>
        <Tasks>
        {
          for(task <- project.linkingModule.tasks) yield exportLinkingTask(task)
        }
        </Tasks>
      </LinkingModule>
    </Project>
  }

  private def exportSourceTask(task : SourceTask) =
  {
    <SourceTask>
    {
      task.source.toXML
    }
    </SourceTask>
  }

  private

  def exportLinkingTask(task : LinkingTask)(implicit prefixes : Prefixes) =
  {
    <LinkingTask>
      <Name>{task.name}</Name>
      <LinkSpecification>{task.linkSpec.toXML}</LinkSpecification>
      <Alignment>{task.alignment.toXML}</Alignment>
      <Cache>{task.cache.toXML}</Cache>
    </LinkingTask>
  }
}