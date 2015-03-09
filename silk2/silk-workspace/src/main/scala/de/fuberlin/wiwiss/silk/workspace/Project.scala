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
import de.fuberlin.wiwiss.silk.workspace.modules.linking.{LinkingTaskExecutor, LinkingModulePlugin}
import de.fuberlin.wiwiss.silk.workspace.modules.dataset.DatasetModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.transform._
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.WorkflowModulePlugin
import de.fuberlin.wiwiss.silk.workspace.modules.{TaskExecutor, Module, ModulePlugin, Task}
import scala.reflect.ClassTag
import scala.xml.XML

/**
 * A project.
 */
class Project(val name: Identifier, val resourceManager: ResourceManager) {

  private implicit val logger = Logger.getLogger(classOf[Project].getName)

  val resources = resourceManager.child("resources")

  @volatile
  private var cachedConfig: Option[ProjectConfig] = None

  @volatile
  private var modules = Seq[Module[_]]()

  @volatile
  private var executors = Map[String, TaskExecutor[_]]()

  // Register all default modules
  registerModule(new DatasetModulePlugin())
  registerModule(new LinkingModulePlugin())
  registerModule(new TransformModulePlugin())
  registerModule(new WorkflowModulePlugin())

  registerExecutor(new LinkingTaskExecutor())
  registerExecutor(new TransformTaskExecutor())

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
   * Retrieves all tasks of a specific type.
   */
  def tasks[T : ClassTag]: Seq[Task[T]] = {
    module[T].tasks
  }

  /**
   * Retrieves a task of a specific type by name.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task[T : ClassTag](taskName: Identifier): Task[T] = {
    module[T].task(taskName)
  }

  /**
   * Retrieves a task of any type by name.
   *
   * @param taskName The name of the task
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def anyTask(taskName: Identifier): Task[_] = {
    modules.flatMap(_.taskOption(taskName).asInstanceOf[Option[Task[_]]]).headOption
           .getOrElse(throw new NoSuchElementException(s"No task '$taskName' found in project '$name'"))
  }

  def addTask[T: ClassTag](name: Identifier, taskData: T) = {
    module[T].add(name, taskData)
  }

  def updateTask[T: ClassTag](name: Identifier, taskData: T) = {
    module[T].taskOption(name) match {
      case Some(task) => task.update(taskData)
      case None => module[T].add(name, taskData)
    }
  }

  /**
   * Removes a task.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   */
  def removeTask[T : ClassTag](taskName: Identifier): Unit = {
    module[T].remove(taskName)
  }

  /**
   * Retrieves an executor for a specific task.
   */
  def getExecutor[T](taskData: T): Option[TaskExecutor[T]] = {
    executors.get(taskData.getClass.getName).map(_.asInstanceOf[TaskExecutor[T]])
  }

  /**
   * Retrieves a module for a specific task type.
   *
   * @tparam T The task type
   * @throws java.util.NoSuchElementException If no module for the given task type has been registered
   */
  private def module[T : ClassTag]: Module[T] = {
    modules.find(_.hasTaskType[T]) match {
      case Some(m) => m.asInstanceOf[Module[T]]
      case None =>
        val className = implicitly[ClassTag[T]].runtimeClass.getName
        throw new NoSuchElementException(s"No module for task type $className has been registered. ${modules.size} Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /**
   * Registers a new module from a module provider.
   */
  def registerModule[T : ClassTag](provider: ModulePlugin[T]) = {
    modules = modules :+ new Module(provider, resourceManager.child(provider.prefix), this)
  }

  /**
   * Registers a new executor for a specific task type.
   */
  def registerExecutor[T : ClassTag](executor: TaskExecutor[T]) = {
    val taskClassName = implicitly[ClassTag[T]].runtimeClass.getName
    executors = executors.updated(taskClassName, executor)
  }
}