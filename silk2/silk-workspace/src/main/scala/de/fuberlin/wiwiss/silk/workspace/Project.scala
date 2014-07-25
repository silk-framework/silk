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

package de.fuberlin.wiwiss.silk.workspace

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.transform._
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowModuleProvider
import de.fuberlin.wiwiss.silk.workspace.modules.{Module, ModuleProvider, ModuleTask}
import scala.reflect.ClassTag
import scala.xml.XML

/**
 * Implementation of a project which is stored on the local file system.
 */
class Project(val name: Identifier, resourceManager: ResourceManager) {

  private implicit val logger = Logger.getLogger(classOf[Project].getName)

  val resources = resourceManager.child("resources")

  @volatile
  private var cachedConfig: Option[ProjectConfig] = None

  @volatile
  private var modules = Seq[Module[_ <: ModuleTask]]()

  // Register all default modules
  registerModule(new DatasetModuleProvider())
  registerModule(new LinkingModuleProvider())
  registerModule(new TransformModuleProvider())
  registerModule(new WorkflowModuleProvider())

  /**
   * Reads the project configuration.
   */
  def config = {
    if(cachedConfig.isEmpty) {
      if(resourceManager.list.contains("config.xml")) {
        val configXML = XML.load(resourceManager.get("config.xml").load)
        val prefixes = Prefixes.fromXML(configXML \ "Prefixes" head)
        cachedConfig = Some(ProjectConfig(prefixes))
      } else {
        cachedConfig = Some(ProjectConfig.default)
      }
    }

    cachedConfig.get
  }

  /**
   * Writes the updated project configuration.
   */
  def config_=(config : ProjectConfig) {
    val configXMl =
      <ProjectConfig>
      { config.prefixes.toXML }
      </ProjectConfig>

    resourceManager.put("config.xml") { os => configXMl.write(os) }
    cachedConfig = Some(config)
  }

  /**
   * Retrieves al tasks of a specific type.
   */
  def tasks[T <: ModuleTask : ClassTag]: Seq[T] = {
    module[T].tasks
  }

  /**
   * Retrieves a task by name.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   */
  def task[T <: ModuleTask : ClassTag](taskName: Identifier): T = {
    module[T].task(taskName)
  }

  def anyTask(taskName: Identifier): ModuleTask = {
     modules.flatMap(_.taskOption(taskName)).head
  }

  /**
   * Updates a task of a specific type.
   *
   * @param task The updated task
   * @tparam T The task type
   */
  def updateTask[T <: ModuleTask : ClassTag](task: T): Unit = {
    // TODO assert that task name is unique
    module[T].update(task)
  }

  /**
   * Removes a task.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   */
  def removeTask[T <: ModuleTask : ClassTag](taskName: Identifier): Unit = {
    module[T].remove(taskName)
  }

  /**
   * Retrieves a module for a specific task type.
   *
   * @tparam T The task type
   * @throws NoSuchElementException If no module for the given task type has been registered
   */
  private def module[T <: ModuleTask : ClassTag]: Module[T] = {
    modules.find(_.hasTaskType[T]) match {
      case Some(m) => m.asInstanceOf[Module[T]]
      case None =>
        val className = implicitly[ClassTag[T]].runtimeClass.getName
        throw new NoSuchElementException(s"No module for task type $className has been registered.")
    }
  }

  /**
   * Registers a new module from a module provider.
   */
  private def registerModule[T <: ModuleTask : ClassTag](provider: ModuleProvider[T]) = {
    modules = modules :+ new Module(provider, resourceManager.child(provider.prefix), this)
  }
}