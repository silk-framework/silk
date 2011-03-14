package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.{Cache, LinkingTask}
import modules.source.SourceTask
import de.fuberlin.wiwiss.silk.datasource.Source
import xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkspec.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.AlignmentReader

/**
 * Reads a project from a single XML file.
 */
object ProjectImporter
{
  def apply(project : Project, xml : NodeSeq) =
  {
    for(taskNode <- xml \ "SourceModule" \ "Tasks" \ "SourceTask")
    {
      project.sourceModule.update(readSourceTask(taskNode))
    }

    for(taskNode <- xml \ "LinkingModule" \ "Tasks" \ "LinkingTask")
    {
      project.linkingModule.update(readLinkingTask(taskNode))
    }
  }

  private def readSourceTask(xml : Node) =
  {
    SourceTask(Source.fromXML(xml \ "_" head))
  }

  private def readLinkingTask(xml : Node) =
  {
    val name = xml \ "Name" text
    implicit val prefixes = Prefixes.fromXML(xml \ "Prefixes" \ "_" head)
    val linkSpec = LinkSpecification.fromXML(xml \ "LinkSpecification" \ "_" head)
    val alignment = AlignmentReader.readAlignment(xml \ "Alignment" \ "_" head)
    val cache = Cache.fromXML(xml \ "Cache" \ "_" head)

    LinkingTask(name, prefixes, linkSpec, alignment, cache)
  }
}