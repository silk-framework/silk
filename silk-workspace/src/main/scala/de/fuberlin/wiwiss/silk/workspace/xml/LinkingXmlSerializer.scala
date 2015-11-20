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

package de.fuberlin.wiwiss.silk.workspace.xml

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, Prefixes}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinksReader
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._

import scala.xml.XML

/**
 * The linking module which encapsulates all linking tasks.
 */
private class LinkingXmlSerializer extends XmlSerializer[LinkSpecification] {

  private val logger = Logger.getLogger(classOf[LinkingXmlSerializer].getName)

  override def prefix = "linking"

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Map[Identifier, LinkSpecification] = {
    val tasks =
      for(name <- resources.listChildren) yield
        loadTask(resources.child(name), projectResources)
    tasks.toMap
  }

  /**
   * Loads a specific task in this module.
   */
  private def loadTask(taskResources: ResourceLoader, projectResources: ResourceManager) = {
    implicit val resources = projectResources
    val linkSpec = fromXml[LinkSpecification](XML.load(taskResources.get("linkSpec.xml").load))
    val referenceLinks = ReferenceLinksReader.readReferenceLinks(taskResources.get("alignment.xml").load)
    (linkSpec.id, linkSpec.copy(referenceLinks = referenceLinks))
  }

  /**
   * Removes a specific task.
   */
  def removeTask(name: Identifier, resources: ResourceManager) = {
    resources.delete(name)
  }

  /**
   * Writes an updated task.
   */
  def writeTask(data: LinkSpecification, resources: ResourceManager) = {
    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    // Write resources
    val taskResources = resources.child(data.id)
    taskResources.get("linkSpec.xml").write(toXml(data).toString())
    taskResources.get("alignment.xml").write{ os => data.referenceLinks.toXML.write(os) }
  }
}