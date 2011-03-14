package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.LinkingTask
import modules.source.SourceTask

/**
 * Exports a project to a single XML file.
 */
object ProjectExporter
{
  def apply(project : Project) =
  {
    <Project>
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

  def exportLinkingTask(task : LinkingTask) =
  {
    //TODO move prefixes to project
    implicit val prefixes = task.prefixes

    <LinkingTask>
      <Name>{task.name}</Name>
      <Prefixes>{task.prefixes.toXML}</Prefixes>
      <LinkSpecification>{task.linkSpec.toXML}</LinkSpecification>
      <Alignment>{task.alignment.toXML}</Alignment>
      <Cache>{task.cache.toXML}</Cache>
    </LinkingTask>
  }
}