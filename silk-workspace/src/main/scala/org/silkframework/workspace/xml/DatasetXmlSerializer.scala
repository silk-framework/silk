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

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlSerialization}
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._
import org.silkframework.workspace.LoadedTask

import java.util.logging.Logger
import scala.xml.Node

/**
 * The source module which encapsulates all data sources.
 */
private class DatasetXmlSerializer extends XmlSerializer[DatasetSpec[Dataset]] {

  private val logger = Logger.getLogger(classOf[DatasetXmlSerializer].getName)

  override def prefix: String = "dataset"

  private def taskNames(resources: ResourceLoader) = {
    resources.list.filter(_.endsWith(".xml")).filter(!_.contains("cache"))
  }

  private def loadTask(resourceName: String,
                       resources: ResourceLoader)
                      (implicit context: PluginContext): LoadedTask[DatasetSpec[Dataset]] = {
    // Load the data set
    implicit val readContext = ReadContext.fromPluginContext()
    loadTaskSafelyFromXML(resourceName, Some(Identifier(resourceName.stripSuffix(".xml"))), resources)
  }

  /**
   * Writes an updated task.
   */
  override def writeTask(task: Task[GenericDatasetSpec], resources: ResourceManager, projectResourceManager: ResourceManager): Unit = {
    // Only serialize file paths correctly, paths should not be prefixed
    implicit val writeContext: WriteContext[Node] = WriteContext[Node](resources = projectResourceManager, prefixes = Prefixes.empty)
    val taskXml = XmlSerialization.toXml(task)
    resources.get(task.id.toString + ".xml").write(){ os => taskXml.write(os) }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString + ".xml")
    resources.delete(name.toString + "_cache.xml")
  }

  override def loadTasks(resources: ResourceLoader)
                        (implicit context: PluginContext): Seq[LoadedTask[GenericDatasetSpec]] = {
    // Read dataset tasks
    val names = taskNames(resources)
    var tasks = for (name <- names) yield {
      loadTask(name, resources)
    }

    // Also read dataset tasks from the old source folder
    if (tasks.isEmpty) {
      val oldResources = resources.parent.get.child("source")
      val oldNames = taskNames(oldResources)
      tasks =
          for (name <- oldNames) yield {
            loadTask(name, oldResources)
          }
    }

    tasks
  }
}