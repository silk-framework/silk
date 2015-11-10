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

package de.fuberlin.wiwiss.silk.workspace.modules.dataset

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.{ModulePlugin, Task, TaskActivity}

import scala.xml.XML

/**
 * The source module which encapsulates all data sources.
 */
class DatasetModulePlugin extends ModulePlugin[Dataset] {

  private val logger = Logger.getLogger(classOf[DatasetModulePlugin].getName)

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

  override def activities(task: Task[Dataset], project: Project): Seq[TaskActivity[_,_]] = {
    // Types cache
    def typesCache() = new TypesCache(task.data)
    // Create task activities
    TaskActivity(s"${task.name}_cache.xml", Types.empty, typesCache, project.cacheResources.child(prefix)) :: Nil
  }
}