/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.datasource.Source
import xml.{Node, NodeSeq}
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.config.LinkSpecification
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinksReader
import de.fuberlin.wiwiss.silk.workbench.workspace.{ProjectConfig, Project}
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.{LinkingTask, Caches}

/**
 * Reads a project from a single XML file.
 */
object ProjectImporter
{
  def apply(project : Project, xml : NodeSeq)
  {
    implicit val prefixes = Prefixes.fromXML(xml \ "Config" \ "Prefixes" head)

    project.config = ProjectConfig(prefixes)

    for(taskNode <- xml \ "SourceModule" \ "Tasks" \ "SourceTask") {
      project.sourceModule.update(readSourceTask(taskNode))
    }

    for(taskNode <- xml \ "LinkingModule" \ "Tasks" \ "LinkingTask") {
      project.linkingModule.update(readLinkingTask(taskNode, project))
    }
  }

  private def readSourceTask(xml : Node) = {
    SourceTask(Source.fromXML(xml \ "_" head))
  }

  private def readLinkingTask(xml : Node, project: Project)(implicit prefixes : Prefixes) = {
    val linkSpec = LinkSpecification.fromXML(xml \ "LinkSpecification" \ "_" head)
    val referenceLinks = ReferenceLinksReader.readReferenceLinks(xml \ "Alignment" \ "_" head)
    val cache = new Caches()
    cache.loadFromXML(xml \ "Cache" \ "_" head)

    LinkingTask(project, linkSpec, referenceLinks, cache)
  }
}