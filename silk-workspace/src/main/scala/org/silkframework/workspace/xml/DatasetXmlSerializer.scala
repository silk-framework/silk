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

import org.silkframework.dataset.Dataset
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.Serialization
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._

import scala.xml.XML

/**
 * The source module which encapsulates all data sources.
 */
private class DatasetXmlSerializer extends XmlSerializer[Dataset] {

  private val logger = Logger.getLogger(classOf[DatasetXmlSerializer].getName)

  override def prefix = "dataset"

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Map[Identifier, Dataset] = {
    // Read dataset tasks
    val names = resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
    var tasks = for (name <- names) yield {
      loadTask(name, resources, projectResources)
    }

    // Also read dataset tasks from the old source folder
    if (tasks.isEmpty) {
      val oldResources = resources.parent.get.child("source")
      val oldNames = oldResources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
      tasks =
        for (name <- oldNames) yield {
          loadTask(name, oldResources, projectResources)
        }
    }

    tasks.toMap
  }

  private def loadTask(name: String, resources: ResourceLoader, projectResources: ResourceManager) = {
    // Load the data set
    implicit val res = projectResources
    val dataset = Serialization.fromXml[Dataset](XML.load(resources.get(name).load))
    (dataset.id, dataset)
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(data: Dataset, resources: ResourceManager): Unit = {
    resources.get(data.id + ".xml").write{ os => Serialization.toXml(data).write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name + ".xml")
    resources.delete(name + "_cache.xml")
  }
}