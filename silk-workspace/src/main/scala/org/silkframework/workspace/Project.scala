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

package org.silkframework.workspace

import java.util.logging.{Level, Logger}

import org.silkframework.config._
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.validation.{NotFoundException, ValidationException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.linking.LinkingTaskExecutor
import org.silkframework.workspace.activity.transform._
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.activity.{ProjectActivity, ProjectActivityFactory, TaskExecutor}

import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * A project.
 */
class Project(initialConfig: ProjectConfig = ProjectConfig(), provider: WorkspaceProvider, val resources: ResourceManager) {

  private implicit val logger = Logger.getLogger(classOf[Project].getName)

  val cacheResources: ResourceManager = provider.projectCache(initialConfig.id)

  @volatile
  private var cachedConfig: ProjectConfig = initialConfig

  @volatile
  private var modules = Seq[Module[_ <: TaskSpec]]()

  @volatile
  private var executors = Map[String, TaskExecutor[_]]()

  /**
    * Holds all issues that occurred during loading project activities.
    */
  @volatile
  private var activityLoadingErrors: Seq[ValidationException] = Seq.empty

  // Register all default modules
  registerModule[DatasetSpec[Dataset]]()
  registerModule[TransformSpec]()
  registerModule[LinkSpec]()
  registerModule[Workflow]()
  registerModule[CustomTask]()

  registerExecutor(new LinkingTaskExecutor())
  registerExecutor(new TransformTaskExecutor())

  // Initialize Tasks
  allTasks.foreach(_.init())

  /**
    * The name of this project.
    */
  def name = cachedConfig.id

  /**
    * Retrieves all errors that occured during loading this project.
    */
  def loadingErrors: Seq[ValidationException] = modules.flatMap(_.loadingError) ++ activityLoadingErrors

  private val projectActivities = {
    val factories = PluginRegistry.availablePlugins[ProjectActivityFactory[_]].toList
    var activities = List[ProjectActivity]()
    for(factory <- factories) {
      try {
        activities ::= new ProjectActivity(this, factory()(config.prefixes, resources))
      } catch {
        case NonFatal(ex) =>
          val errorMsg = s"Could not load project activity '$factory' in project '${initialConfig.id}'."
          activityLoadingErrors :+= new ValidationException(errorMsg + "Details: " + ex.getMessage, ex)
          logger.log(Level.WARNING, errorMsg, ex)
      }
    }
    activities.reverse
  }

  /**
    * Available activities for this project.
    */
  def activities: Seq[ProjectActivity] = {
    projectActivities
  }

  /**
    * Retrieves an activity by name.
    *
    * @param activityName The name of the requested activity
    * @return The activity control for the requested activity
    * @throws org.silkframework.runtime.validation.NotFoundException
    */
  def activity(activityName: String) = {
    projectActivities.find(_.name == activityName)
      .getOrElse(throw NotFoundException(s"Project '$name' does not contain an activity named '$activityName'. " +
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
  def allTasks: Seq[ProjectTask[_ <: TaskSpec]] = {
    for(module <- modules; task <- module.tasks) yield task.asInstanceOf[ProjectTask[_ <: TaskSpec]]
  }

  /**
   * Retrieves all tasks of a specific type.
   */
  def tasks[T <: TaskSpec : ClassTag]: Seq[ProjectTask[T]] = {
    val targetType = implicitly[ClassTag[T]].runtimeClass
    module[T].tasks.filter(task => targetType.isAssignableFrom(task.data.getClass))
  }

  /**
   * Retrieves a task of a specific type by name.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task[T <: TaskSpec : ClassTag](taskName: Identifier): ProjectTask[T] = {
    module[T].task(taskName)
  }

  def taskOption[T <: TaskSpec : ClassTag](taskName: Identifier): Option[ProjectTask[T]] = {
    module[T].taskOption(taskName)
  }

  /**
   * Retrieves a task of any type by name.
   *
   * @param taskName The name of the task
   * @throws org.silkframework.workspace.TaskNotFoundException If no task with the given name has been found
   */
  def anyTask(taskName: Identifier): ProjectTask[_ <: TaskSpec] = {
    modules.flatMap(_.taskOption(taskName).asInstanceOf[Option[ProjectTask[_ <: TaskSpec]]]).headOption
           .getOrElse(throw TaskNotFoundException(config.id, taskName, "Task"))
  }

  /**
    * Retrieves a task of any type by name if it exists, else it returns None.
    *
    * @param taskName The name of the task
    */
  def anyTaskOption(taskName: Identifier): Option[ProjectTask[_ <: TaskSpec]] = {
    modules.flatMap(_.taskOption(taskName).asInstanceOf[Option[ProjectTask[_ <: TaskSpec]]]).headOption
  }

  /**
    * Adds a new task to this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    * @tparam T The task type.
    */
  def addTask[T <: TaskSpec : ClassTag](name: Identifier, taskData: T, metaData: MetaData = MetaData.empty): Unit = synchronized {
    if(allTasks.exists(_.id == name)) {
      throw IdentifierAlreadyExistsException(s"Task name '$name' is not unique as there is already a task in project '${this.name}' with this name.")
    }
    module[T].add(name, taskData, metaData)
  }

  /**
    * Adds a new task of any type to this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    */
  def addAnyTask(name: Identifier, taskData: TaskSpec, metaData: MetaData = MetaData.empty): Unit = synchronized {
    if(allTasks.exists(_.id == name)) {
      throw IdentifierAlreadyExistsException(s"Task name '$name' is not unique as there is already a task in project '${this.name}' with this name.")
    }
    modules.find(_.taskType.isAssignableFrom(taskData.getClass)) match {
      case Some(module) => module.asInstanceOf[Module[TaskSpec]].add(name, taskData, metaData)
      case None => throw new NoSuchElementException(s"No module for task type ${taskData.getClass} has been registered. Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /**
    * Updates a task.
    * If no task with the given name exists, a new task is created in the project.
    *
    * @param name The name of the task.
    * @param taskData The task data.
    * @tparam T The task type.
    */
  def updateTask[T <: TaskSpec : ClassTag](name: Identifier, taskData: T, metaData: MetaData = MetaData.empty): Unit = synchronized {
    module[T].taskOption(name) match {
      case Some(task) =>
        task.update(taskData, Some(metaData))
      case None =>
        addTask[T](name, taskData, metaData)
    }
  }

  /**
    * Updates a task of any type in this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    */
  def updateAnyTask(name: Identifier, taskData: TaskSpec, metaData: MetaData = MetaData.empty): Unit = synchronized {
    modules.find(_.taskType.isAssignableFrom(taskData.getClass)) match {
      case Some(module) =>
        module.taskOption(name) match {
          case Some(task) =>
            task.asInstanceOf[ProjectTask[TaskSpec]].update(taskData, Some(metaData))
          case None =>
            addAnyTask(name, taskData, metaData)
        }
      case None =>
        throw new NoSuchElementException(s"No module for task type ${taskData.getClass} has been registered. Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /**
   * Removes a task of a specific type.
   * Note that the named task will be deleted, even if it is referenced by another task.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   */
  def removeTask[T <: TaskSpec : ClassTag](taskName: Identifier): Unit = synchronized {
    module[T].remove(taskName)
  }

  /**
    * Removes a task of any type.
    *
    * @param taskName The name of the task
    * @param removeDependentTasks Also remove tasks that directly or indirectly reference the named task
    * @throws ValidationException If the task to be removed is referenced by another task and removeDependentTasks is false.
    */
  def removeAnyTask(taskName: Identifier, removeDependentTasks: Boolean): Unit = synchronized {
    if(removeDependentTasks) {
      // Remove all dependent tasks
      for(dependentTask <- anyTask(taskName).findDependentTasks(recursive = false) if anyTaskOption(dependentTask).isDefined) {
        removeAnyTask(dependentTask, removeDependentTasks = true)
      }
    } else {
      // Make sure that no other task depends on this task
      for(task <- allTasks) {
        if(task.data.inputTasks.contains(taskName)) {
          throw new ValidationException(s"Cannot delete task $taskName as it is referenced by task ${task.id}")
        }
      }
    }

    // Find the module which holds the named task and remove it
    for(m <- modules.find(_.taskOption(taskName).isDefined)) {
      m.remove(taskName)
    }
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
  private def module[T <: TaskSpec : ClassTag]: Module[T] = {
    modules.find(_.hasTaskType[T]) match {
      case Some(m) => m.asInstanceOf[Module[T]]
      case None =>
        val className = implicitly[ClassTag[T]].runtimeClass.getName
        throw new NoSuchElementException(s"No module for task type $className has been registered. Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /**
   * Registers a new module from a module provider.
   */
  def registerModule[T <: TaskSpec : ClassTag](): Unit = synchronized {
    modules = modules :+ new Module[T](provider, this)
  }

  /**
   * Registers a new executor for a specific task type.
   */
  def registerExecutor[T : ClassTag](executor: TaskExecutor[T]): Unit = {
    val taskClassName = implicitly[ClassTag[T]].runtimeClass.getName
    executors = executors.updated(taskClassName, executor)
  }

  /** Flush outstanding updates */
  def flush(): Unit = synchronized {
    for(task <- allTasks) {
      try {
        task.flush()
      } catch {
        case NonFatal(ex) =>
          logger.warning(s"Could not persist task ${task.id} of project ${config.id} to workspace provider. Reason: " + ex.getMessage)
      }
    }
  }
}