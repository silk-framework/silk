package org.silkframework.workspace

import org.silkframework.config.{CustomTask, MetaData, PlainTask, TaskSpec}
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.rule.{LinkSpec, RuleBlockSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariables}
import org.silkframework.util.Identifier
import org.silkframework.workspace.TaskCleanupPlugin.CleanUpAfterTaskDeletionFunction
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.changes.{AddTask, RemoveTask}
import org.silkframework.workspace.exceptions.TaskNotFoundException

import java.util.logging.{Level, Logger}
import scala.collection.immutable.TreeMap
import scala.reflect.ClassTag
import scala.util.control.NonFatal

/**
  * A module holds all tasks of a specific type.
  *
  * @param provider The workspace provider
  * @param project The project this module belongs to
  * @tparam TaskData The task type held by this module
  */
class Module[TaskData <: TaskSpec: ClassTag](private[workspace] val provider: WorkspaceProvider,
                                             private[workspace] val project: Project,
                                             private[workspace] val validator: TaskValidator[TaskData] = new DefaultTaskValidator[TaskData]) {

  private val logger = Logger.getLogger(classOf[Module[_]].getName)

  lazy val cleanUpAfterTaskDeletion: CleanUpAfterTaskDeletionFunction = {
    TaskCleanupPlugin.retrieveCleanUpAfterTaskDeletionFunction
  }

  /**
   * Caches all tasks of this module in memory.
   */
  @volatile
  private var cachedTasks: TreeMap[Identifier, ProjectTask[TaskData]] = null

  /**
    * Holds all issues that occurred during loading.
    */
  @volatile
  private var errors: List[TaskLoadingError] = List.empty

  /**
    * Returns a validation exception if an error occured during task loading.
    */
  private[workspace] def loadingErrors: List[TaskLoadingError] = errors

  private[workspace] def removeLoadingError(taskId: Identifier): Unit = synchronized {
    errors = errors.filterNot(_.taskId == taskId)
  }

  def hasTaskType[T : ClassTag]: Boolean = {
    implicitly[ClassTag[TaskData]].runtimeClass.isAssignableFrom(implicitly[ClassTag[T]].runtimeClass)
  }

  val taskType: Class[_] = {
    implicitly[ClassTag[TaskData]].runtimeClass
  }

  override def toString: String = {
    s"Module[${taskType.getSimpleName}]"
  }

  /**
   * Retrieves all tasks in this module.
   */
  def tasks(implicit userContext: UserContext): Seq[ProjectTask[TaskData]] = {
    assertLoaded()
    cachedTasks.values.toSeq
  }

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier)
          (implicit userContext: UserContext): ProjectTask[TaskData] = {
    assertLoaded()
    cachedTasks.getOrElse(name, throw TaskNotFoundException(project.id, name, Module.taskTypeName(taskType)))
  }

  def taskOption(name: Identifier)
                (implicit userContext: UserContext): Option[ProjectTask[TaskData]] = {
    assertLoaded()
    cachedTasks.get(name)
  }

  def add(name: Identifier, taskData: TaskData, metaData: MetaData, executionVariables: TemplateVariables = TemplateVariables.empty)
         (implicit userContext: UserContext) : ProjectTask[TaskData] = {
    assertLoaded()
    // Variable templates are resolved at save time; unresolvable templates keep the provided value.
    val parentVariables = (GlobalTemplateVariables.all merge project.templateVariables.all).withoutSensitiveVariables()
    val resolvedVariables = executionVariables.resolvedKeepingUnresolved(parentVariables)
    val task = new ProjectTask(name, taskData, metaData, resolvedVariables, this)
    task.executionVariablesValueHolder.validateScope(resolvedVariables)
    validator.validate(project, task)
    provider.putTask(project.id, task, project.resources)
    task.startActivities()
    cachedTasks += ((name, task))
    project.changeJournal.record(AddTask(PlainTask.fromTask(task)))
    logger.info(s"Added task '$name' to project ${project.id}." + userContext.logInfo)
    task
  }

  /**
   * Removes a task from this module.
   */
  def remove(taskId: Identifier)
            (implicit userContext: UserContext): Unit = {
    assertLoaded()
    // Cancel all activities
    for {
      task <- cachedTasks.get(taskId)
      activity <- task.activities
    } {
      activity.control.cancel()
    }
    // Delete task
    val taskOpt = taskOption(taskId)
    provider.deleteTask(project.id, taskId)
    cachedTasks -= taskId
    taskOpt.foreach(task => project.changeJournal.record(RemoveTask(PlainTask.fromTask(task))))
    taskOpt.foreach(task => cleanUpAfterTaskDeletion(project.id, taskId, task))
    logger.info(s"Removed task '$taskId' from project ${project.id}." + userContext.logInfo)
  }

  /**
    * Loads the tasks in this module.
    * Has to be called initially.
    */
  def load(tasks: Seq[LoadedTask[TaskData]])
          (implicit userContext: UserContext): Unit = synchronized {
    if(cachedTasks == null) {
      try {
        logger.fine(s"Loading tasks of type ${taskType.getSimpleName}")
        cachedTasks = TreeMap()(TaskOrdering) ++ {
          (for (taskTry <- tasks) yield {
            taskTry.taskOrError match {
              case Right(task) =>
                Some((task.id, new ProjectTask(task.id, task.data, task.metaData, task.executionVariables, this)))
              case Left(taskLoadingError) =>
                errors ::= taskLoadingError
                None
            }
          }).flatten
        }
        handleTaskExceptions()
      } catch {
        case NonFatal(ex) =>
          handleUnexpectedException(ex)
      }
    }
  }

  private def handleUnexpectedException(ex: Throwable): Unit = {
    cachedTasks = TreeMap()(TaskOrdering)
    logger.log(Level.WARNING, s"Error loading tasks of type ${taskType.getName}", ex)
  }

  private def handleTaskExceptions(): Unit = {
    for (loadingError <- errors) {
      logger.log(Level.WARNING, s"Error loading tasks of type ${taskType.getName}", loadingError.throwable)
    }
  }

  private def assertLoaded(): Unit = {
    if(cachedTasks == null) {
      throw new Exception("Tried to access tasks before Module has been loaded")
    }
  }

  /**
   * Defines how tasks are sorted based on their identifier.
   */
  private object TaskOrdering extends Ordering[Identifier] {
    def compare(a:Identifier, b:Identifier): Int = a.toString.compareTo(b.toString)
  }
}

object Module {

  /** Task types as task JSON names them (JsonSerializers.TASK_TYPE_*, which this module cannot import). */
  private val taskTypeNames: Seq[(Class[_], String)] = Seq(
    classOf[DatasetSpec[Dataset]] -> "Dataset", classOf[TransformSpec] -> "Transform", classOf[LinkSpec] -> "Linking",
    classOf[RuleBlockSpec] -> "RuleBlock", classOf[Workflow] -> "Workflow", classOf[CustomTask] -> "CustomTask")

  /** The API name of a task type, so a message names what a caller sent rather than the Scala class. */
  def taskTypeName(taskType: Class[_]): String = {
    taskTypeNames.collectFirst { case (cls, name) if cls.isAssignableFrom(taskType) => name }.getOrElse(taskType.getSimpleName)
  }
}
