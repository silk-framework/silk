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

package org.silkframework.workspace.xml

import java.util.logging.Logger

import org.silkframework.config.{CustomTaskSpecification, Prefixes}
import org.silkframework.dataset.Dataset
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._

import scala.xml.XML

/**
 * Holds custom tasks.
 */
private class CustomTaskXmlSerializer extends XmlSerializer[CustomTaskSpecification] {

  private val logger = Logger.getLogger(classOf[CustomTaskXmlSerializer].getName)

  override def prefix = "custom"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Map[Identifier, CustomTaskSpecification] = {
    val names = resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
    val tasks = for (name <- names) yield {
      loadTask(name, resources, projectResources)
    }

    tasks.toMap
  }

  private def loadTask(name: String, resources: ResourceLoader, projectResources: ResourceManager) = {
    implicit val res = projectResources
    implicit val readContext = ReadContext(projectResources)
    val taskSpec = XmlSerialization.fromXml[CustomTaskSpecification](XML.load(resources.get(name).load))
    (taskSpec.id, taskSpec)
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(data: CustomTaskSpecification, resources: ResourceManager): Unit = {
    resources.get(data.id.toString + ".xml").write{ os => XmlSerialization.toXml(data).write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString + ".xml")
    resources.delete(name.toString + "_cache.xml")
  }
}