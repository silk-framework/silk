package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.datasource.Source
import xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinksReader
import de.fuberlin.wiwiss.silk.workbench.workspace.{ProjectConfig, Project}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.{LinkingTask, Cache}

/**
 * Reads a project from a single XML file.
 */
object ProjectImporter
{
  def apply(project : Project, xml : NodeSeq)
  {
    implicit val prefixes = Prefixes.fromXML(xml \ "Config" \ "Prefixes" head)

    project.config = ProjectConfig(prefixes)

    for(taskNode <- xml \ "SourceModule" \ "Tasks" \ "SourceTask")
    {
      project.sourceModule.update(readSourceTask(taskNode))
    }

    for(taskNode <- xml \ "LinkingModule" \ "Tasks" \ "LinkingTask")
    {
      project.linkingModule.update(readLinkingTask(taskNode, project))
    }
  }

  private def readSourceTask(xml : Node) =
  {
    SourceTask(Source.fromXML(xml \ "_" head))
  }

  private def readLinkingTask(xml : Node, project: Project)(implicit prefixes : Prefixes) =
  {
    val linkSpec = LinkSpecification.fromXML(xml \ "LinkSpecification" \ "_" head)
    val referenceLinks = ReferenceLinksReader.readReferenceLinks(xml \ "Alignment" \ "_" head)

    lazy val task: LinkingTask = LinkingTask(linkSpec, referenceLinks)
    task.cache.fromXML(xml \ "Cache" \ "_" head, project, task)
    task.cache.load(project, task)
    task
  }
}