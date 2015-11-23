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

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, TransformSpecification}
import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityControl, Activity}
import de.fuberlin.wiwiss.silk.runtime.plugin.PluginRegistry
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceManager
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workspace.modules.linking.LinkingTaskExecutor
import de.fuberlin.wiwiss.silk.workspace.modules.transform._
import de.fuberlin.wiwiss.silk.workspace.modules.workflow.Workflow
import de.fuberlin.wiwiss.silk.workspace.modules.{TaskActivity, Module, Task, TaskExecutor}
import de.fuberlin.wiwiss.silk.workspace.xml._

import scala.reflect.ClassTag

/**
 * A project.
 */
class Project(initialConfig: ProjectConfig, provider: WorkspaceProvider) {

  private implicit val logger = Logger.getLogger(classOf[Project].getName)

  val resources = provider.projectResources(initialConfig.id)

  val cacheResources = provider.projectCache(initialConfig.id)

  @volatile
  private var cachedConfig: ProjectConfig = initialConfig

  @volatile
  private var modules = Seq[Module[_]]()

  @volatile
  private var executors = Map[String, TaskExecutor[_]]()

  // Register all default modules
  registerModule[Dataset]()
  registerModule[TransformSpecification]()
  registerModule[LinkSpecification]()
  registerModule[Workflow]()

  registerExecutor(new LinkingTaskExecutor())
  registerExecutor(new TransformTaskExecutor())

  /**
    * The name of this project.
    */
  def name = cachedConfig.id

  /**
    * Available activities for this project.
    */
  val activities: Seq[ActivityControl[Unit]] = {
    for { activityProvider <- PluginRegistry.availablePlugins[ActivityProvider].toList
          activity <- activityProvider().projectActivities(this) } yield Activity(activity)
  }

  /**
    * Retrieves an activity by name.
    *
    * @param activityName The name of the requested activity
    * @return The activity control for the requested activity
    */
  def activity(activityName: String): ActivityControl[_] = {
    activities.find(_.name == activityName)
      .getOrElse(throw new NoSuchElementException(s"Project '$name' does not contain an activity named '$activityName'. " +
        s"Available activities: ${activities.map(_.name).mkString(", ")}"))
  }

  /**
   * Reads the project configuration.
   */
  def config: ProjectConfig = cachedConfig

  /**
   * Writes the updated project configuration.
   */
  def config_=(project : ProjectConfig) {
    provider.putProject(project)
    cachedConfig = project
  }

  /**
   * Retrieves all tasks in this project.
   */
  def allTasks: Seq[Task[_]] = for(module <- modules; task <- module.tasks) yield task.asInstanceOf[Task[_]]

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

  def taskOption[T : ClassTag](taskName: Identifier): Option[Task[T]] = {
    module[T].taskOption(taskName)
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
  def registerModule[T : ClassTag]() = {
    modules = modules :+ new Module[T](provider, this)
  }

  /**
   * Registers a new executor for a specific task type.
   */
  def registerExecutor[T : ClassTag](executor: TaskExecutor[T]) = {
    val taskClassName = implicitly[ClassTag[T]].runtimeClass.getName
    executors = executors.updated(taskClassName, executor)
  }
}