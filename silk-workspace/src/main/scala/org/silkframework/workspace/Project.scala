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

import org.silkframework.config._
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.templating.TemplateVariablesManager
import org.silkframework.runtime.validation.{NotFoundException, ValidationException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.{Workflow, WorkflowValidator}
import org.silkframework.workspace.activity.{ProjectActivity, ProjectActivityFactory}
import org.silkframework.workspace.exceptions.{IdentifierAlreadyExistsException, TaskNotFoundException}

import java.util.logging.{Level, Logger}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
 * A project.
 */
class Project(initialConfig: ProjectConfig, provider: WorkspaceProvider, val resources: ResourceManager)
             (implicit userContext: UserContext) extends ProjectTrait {

  private implicit val logger: Logger = Logger.getLogger(classOf[Project].getName)

  val tagManager = new TagManager(initialConfig.id, provider)

  val templateVariables: TemplateVariablesManager = new ProjectTemplateVariablesManager(provider.projectVariables(initialConfig.id))

  val cacheResources: ResourceManager = provider.projectCache(initialConfig.id)

  @volatile
  private var cachedConfig: ProjectConfig = initialConfig

  @volatile
  private var modules = Seq[Module[_ <: TaskSpec]]()

  loadTasks()

  /** Initializes the project, i.e. registers modules and loads tasks. */
  private def loadTasks()(implicit userContext: UserContext): Unit = {
    // Register all default modules
    registerModule[DatasetSpec[Dataset]]()
    registerModule[TransformSpec]()
    registerModule[LinkSpec]()
    registerModule[Workflow](WorkflowValidator)
    registerModule[CustomTask]()

    // Load tasks
    implicit val pluginContext: PluginContext = PluginContext.fromProject(this)
    val tasks = provider.readAllTasks(id)
    for(module <- modules) {
      val moduleTasks = tasks.filter(task => module.taskType.isAssignableFrom(task.taskType)).asInstanceOf[Seq[LoadedTask[TaskSpec]]]
      module.asInstanceOf[Module[TaskSpec]].load(moduleTasks)
    }
  }

  /** This must be executed once when the project was loaded into the workspace */
  def startActivities()(implicit userContext: UserContext): Unit = {
    allTasks.foreach(_.startActivities())
  }

  /**
    * Cancels all activities in this project.
    */
  def cancelActivities()(implicit userContext: UserContext): Unit = {
    // Project activities
    activities.foreach(_.control.cancel())
    // Task activities
    allTasks.foreach(_.cancelActivities())
  }

  /**
    * The name of this project.
    */
  override def id: Identifier = cachedConfig.id

  /**
    * Retrieves all errors that occurred during loading this project.
    */
  def loadingErrors: Seq[TaskLoadingError] = {
    val errors = modules.flatMap(_.loadingError)
    val errorIds = errors.map(_.taskId).toSet
    val externalLoadingErrors = provider.externalTaskLoadingErrors(config.id).filterNot(extError => errorIds.contains(extError.taskId))
    // Some workspaces have duplicate loading errors, make them distinct.
    (errors ++ externalLoadingErrors).distinctBy(_.taskId)
  }

  private val projectActivities = {
    implicit val pluginContext: PluginContext = PluginContext(prefixes = config.prefixes, resources = resources, user = userContext)
    val factories = PluginRegistry.availablePlugins[ProjectActivityFactory[_ <: HasValue]].toList
    var activities = List[ProjectActivity[_ <: HasValue]]()
    for(factory <- factories) {
      try {
        activities ::= new ProjectActivity(this, factory())
      } catch {
        case NonFatal(ex) =>
          val errorMsg = s"Could not load project activity '$factory' in project '${initialConfig.id}'."
          logger.log(Level.WARNING, errorMsg, ex)
      }
    }
    activities.reverse
  }

  /**
    * Available activities for this project.
    */
  def activities: Seq[ProjectActivity[_ <: HasValue]] = {
    projectActivities
  }

  /**
    * Retrieves an activity by name.
    *
    * @param activityName The name of the requested activity
    * @return The activity control for the requested activity
    * @throws NotFoundException
    */
  def activity(activityName: String): ProjectActivity[_ <: HasValue] = {
    projectActivities.find(_.name.toString == activityName)
      .getOrElse(throw NotFoundException(s"Project '$id' does not contain an activity named '$activityName'. " +
        s"Available activities: ${activities.map(_.name).mkString(", ")}"))
  }

  /**
   * Reads the project configuration.
   */
  def config: ProjectConfig = cachedConfig

  /**
   * Writes the updated project configuration.
   */
  def config_=(project : ProjectConfig)(implicit userContext: UserContext) {
    provider.putProject(project)
    logger.info(s"Project meta data updated for ${project.labelAndId()}.")
    cachedConfig = project
  }

  /**
    * Adds additional prefixes that are defined for the whole workspace and are not persisted with the project.
    * Project prefixes overwrite workspace prefixes.
    *
    * @param workspacePrefixes The prefixes that should be added to the project config.
    */
  def setWorkspacePrefixes(workspacePrefixes: Prefixes) {
    cachedConfig = cachedConfig.copy(projectPrefixes = cachedConfig.projectPrefixes, workspacePrefixes = workspacePrefixes)
  }

  /** Update the meta data of a project. */
  def updateMetaData(metaData: MetaData)
                    (implicit userContext: UserContext): MetaData = synchronized {
    val projectConfig = config
    val mergedMetaData = config.metaData.copy(label = metaData.label, description = metaData.description, tags = metaData.tags)
    val updatedProjectConfig = projectConfig.copy(metaData = mergedMetaData.asUpdatedMetaData)
    config = updatedProjectConfig
    updatedProjectConfig.metaData
  }

  /**
   * Retrieves all tasks in this project.
   */
  def allTasks(implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    for(module <- modules; task <- module.tasks) yield task.asInstanceOf[ProjectTask[_ <: TaskSpec]]
  }

  /**
   * Retrieves all tasks of a specific type.
   */
  override def tasks[T <: TaskSpec : ClassTag](implicit userContext: UserContext): Seq[ProjectTask[T]] = {
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
  def task[T <: TaskSpec : ClassTag](taskName: Identifier)
                                    (implicit userContext: UserContext): ProjectTask[T] = {
    module[T].task(taskName)
  }

  override def taskOption[T <: TaskSpec : ClassTag](taskName: Identifier)
                                          (implicit userContext: UserContext): Option[ProjectTask[T]] = {
    module[T].taskOption(taskName)
  }

  /**
   * Retrieves a task of any type by name.
   *
   * @param taskName The name of the task
   * @throws TaskNotFoundException If no task with the given name has been found
   */
  def anyTask(taskName: Identifier)
             (implicit userContext: UserContext): ProjectTask[_ <: TaskSpec] = {
    modules.flatMap(_.taskOption(taskName).asInstanceOf[Option[ProjectTask[_ <: TaskSpec]]]).headOption
           .getOrElse(throw TaskNotFoundException(config.id, taskName, "Task"))
  }

  /**
    * Retrieves a task of any type by name if it exists, else it returns None.
    *
    * @param taskName The name of the task
    */
  def anyTaskOption(taskName: Identifier)
                   (implicit userContext: UserContext): Option[ProjectTask[_ <: TaskSpec]] = {
    modules.flatMap(_.taskOption(taskName).asInstanceOf[Option[ProjectTask[_ <: TaskSpec]]]).headOption
  }

  /**
    * Adds a new task to this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    * @tparam T The task type.
    */
  def addTask[T <: TaskSpec : ClassTag](name: Identifier, taskData: T, metaData: MetaData = MetaData.empty)
                                       (implicit userContext: UserContext): ProjectTask[T] = synchronized {
    if(allTasks.exists(_.id == name)) {
      throw IdentifierAlreadyExistsException(s"Task name '$name' is not unique as there is already a task in project '${this.id}' with this name.")
    }
    val task = module[T].add(name, taskData, metaData.asNewMetaData)
    provider.removeExternalTaskLoadingError(config.id, name)
    task
  }

  /**
    * Adds a new task of any type to this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    */
  def addAnyTask(name: Identifier, taskData: TaskSpec, metaData: MetaData = MetaData.empty)
                (implicit userContext: UserContext): ProjectTask[TaskSpec] = synchronized {
    if(allTasks.exists(_.id == name)) {
      throw IdentifierAlreadyExistsException(s"Task name '$name' is not unique as there is already a task in project '${this.id}' with this name.")
    }
    modules.find(_.taskType.isAssignableFrom(taskData.getClass)) match {
      case Some(module) => module.asInstanceOf[Module[TaskSpec]].add(name, taskData, metaData.asNewMetaData)
      case None => throw new NoSuchElementException(s"No module for task type ${taskData.getClass} has been registered. Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /**
    * Updates a task.
    * If no task with the given name exists, a new task is created in the project.
    *
    * @param name The name of the task.
    * @param taskData The task data.
    * @param metaData The task meta data. If not provided, no changes to the meta data are made.
    * @tparam T The task type.
    */
  def updateTask[T <: TaskSpec : ClassTag](name: Identifier, taskData: T, metaData: Option[MetaData] = None)
                                          (implicit userContext: UserContext): ProjectTask[T] = synchronized {
    module[T].taskOption(name) match {
      case Some(task) =>
        val mergedMetaData = mergeMetaData(task.metaData, metaData)
        task.update(taskData, Some(mergedMetaData.asUpdatedMetaData))
        task
      case None =>
        addTask[T](name, taskData, metaData.getOrElse(MetaData.empty).asNewMetaData)
    }
  }

  private def mergeMetaData(metaData: MetaData, fromMetaData: Option[MetaData]): MetaData = {
    fromMetaData match {
      case Some(newMetaData) => metaData.copy(label = newMetaData.label, description = newMetaData.description)
      case None => metaData
    }
  }

  /**
    * Updates a task of any type in this project.
    *
    * @param name The name of the task. Must be unique for all tasks in this project.
    * @param taskData The task data.
    * @param metaData The task meta data. If not provided, no changes to the meta data are made.
    */
  def updateAnyTask(name: Identifier, taskData: TaskSpec, metaData: Option[MetaData] = None)
                   (implicit userContext: UserContext): Unit = synchronized {
    modules.find(_.taskType.isAssignableFrom(taskData.getClass)) match {
      case Some(module) =>
        module.taskOption(name) match {
          case Some(task) =>
            val mergedMetaData = mergeMetaData(task.metaData, metaData)
            task.asInstanceOf[ProjectTask[TaskSpec]].update(taskData, Some(mergedMetaData.asUpdatedMetaData))
          case None =>
            addAnyTask(name, taskData, metaData.getOrElse(MetaData.empty).asNewMetaData)
        }
      case None =>
        throw new NoSuchElementException(s"No module for task type ${taskData.getClass} has been registered. Registered task types: ${modules.map(_.taskType).mkString(";")}")
    }
  }

  /** Removes a task loading error. */
  def removeLoadingError(taskId: String): Unit = {
    modules.foreach(_.removeLoadingError(taskId))
    provider.removeExternalTaskLoadingError(id, taskId)
  }

  /**
   * Removes a task of a specific type.
   * Note that the named task will be deleted, even if it is referenced by another task.
   *
   * @param taskName The name of the task
   * @tparam T The task type
   */
  def removeTask[T <: TaskSpec : ClassTag](taskName: Identifier)
                                          (implicit userContext: UserContext): Unit = synchronized {
    module[T].remove(taskName)
  }

  /**
    * Removes a task of any type.
    *
    * @param taskName The name of the task
    * @param removeDependentTasks Also remove tasks that directly or indirectly reference the named task
    * @throws ValidationException If the task to be removed is referenced by another task and removeDependentTasks is false.
    */
  def removeAnyTask(taskName: Identifier, removeDependentTasks: Boolean)
                   (implicit userContext: UserContext): Unit = synchronized {
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
  def registerModule[T <: TaskSpec : ClassTag](validator: TaskValidator[T] = new DefaultTaskValidator[T]): Unit = synchronized {
    modules = modules :+ new Module[T](provider, this, validator)
  }

  /**
    * Retrieves all modules
    */
  def registeredModules: Seq[Module[_ <: TaskSpec]] = modules

  /**
    * Retrieves all tags for this project.
    */
  override def tags()(implicit userContext: UserContext): Set[Tag] = {
    config.metaData.tags.map(uri => tagManager.getTag(uri))
  }

  override def toString: String = s"Project ${config.labelAndId(config.prefixes)}"
}