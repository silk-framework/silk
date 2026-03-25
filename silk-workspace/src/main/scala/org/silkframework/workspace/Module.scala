package org.silkframework.workspace

import org.silkframework.config.{MetaData, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.TaskCleanupPlugin.CleanUpAfterTaskDeletionFunction
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
  private[workspace] def loadingError: List[TaskLoadingError] = errors

  private[workspace] def removeLoadingError(taskId: Identifier): Unit = synchronized {
    errors = errors.filterNot(_.taskId == taskId)
  }

  def hasTaskType[T : ClassTag]: Boolean = {
    implicitly[ClassTag[TaskData]].runtimeClass.isAssignableFrom(implicitly[ClassTag[T]].runtimeClass)
  }

  val taskType: Class[_] = {
    implicitly[ClassTag[TaskData]].runtimeClass
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
    cachedTasks.getOrElse(name, throw TaskNotFoundException(project.id, name, taskType.getSimpleName))
  }

  def taskOption(name: Identifier)
                (implicit userContext: UserContext): Option[ProjectTask[TaskData]] = {
    assertLoaded()
    cachedTasks.get(name)
  }

  def add(name: Identifier, taskData: TaskData, metaData: MetaData)
         (implicit userContext: UserContext) : ProjectTask[TaskData] = {
    assertLoaded()
    val task = new ProjectTask(name, taskData, metaData, this)
    validator.validate(project, task)
    provider.putTask(project.id, task, project.resources)
    task.startActivities()
    cachedTasks += ((name, task))
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
                Some((task.id, new ProjectTask(task.id, task.data, task.metaData, this)))
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