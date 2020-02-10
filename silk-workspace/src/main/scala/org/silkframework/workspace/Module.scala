package org.silkframework.workspace

import java.util.logging.{Level, Logger}

import org.silkframework.config.{MetaData, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier

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
                                             private[workspace] val project: Project) {

  private val logger = Logger.getLogger(classOf[Module[_]].getName)

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

  def hasTaskType[T : ClassTag]: Boolean = {
    implicitly[ClassTag[TaskData]].runtimeClass.isAssignableFrom(implicitly[ClassTag[T]].runtimeClass)
  }

  def taskType: Class[_] = {
    implicitly[ClassTag[TaskData]].runtimeClass
  }

  /**
   * Retrieves all tasks in this module.
   */
  def tasks(implicit userContext: UserContext): Seq[ProjectTask[TaskData]] = {
    load()
    cachedTasks.values.toSeq
  }

  /**
   * Retrieves a task by name.
   *
   * @throws java.util.NoSuchElementException If no task with the given name has been found
   */
  def task(name: Identifier)
          (implicit userContext: UserContext): ProjectTask[TaskData] = {
    load()
    cachedTasks.getOrElse(name, throw TaskNotFoundException(project.name, name, taskType.getName))
  }

  def taskOption(name: Identifier)
                (implicit userContext: UserContext): Option[ProjectTask[TaskData]] = {
    load()
    cachedTasks.get(name)
  }

  def add(name: Identifier, taskData: TaskData, metaData: MetaData)
         (implicit userContext: UserContext): Unit = {
    val task = new ProjectTask(name, taskData, metaData, this)
    provider.putTask(project.name, task)
    task.startActivities()
    cachedTasks += ((name, task))
    logger.info(s"Added task '$name' to project ${project.name}. " + userContext.logInfo)
  }

  /**
   * Removes a task from this module.
   */
  def remove(taskId: Identifier)
            (implicit userContext: UserContext){
    // Cancel all activities
    for {
      task <- cachedTasks.get(taskId)
      activity <- task.activities
    } {
      activity.control.cancel()
    }
    // Delete task
    provider.deleteTask(project.name, taskId)
    cachedTasks -= taskId
    logger.info(s"Removed task '$taskId' from project ${project.name}." + userContext.logInfo)
  }

  /**
    * Loads the tasks in this module.
    * Will be triggered automatically the first time a task is requested.
    * Can be triggered manually to control when the tasks are loaded.
    */
  def load()
                  (implicit userContext: UserContext): Unit = synchronized {
    if(cachedTasks == null) {
      try {
        val tasks = provider.readTasksSafe[TaskData](project.name, project.resources)
        cachedTasks = TreeMap()(TaskOrdering) ++ {
          (for (taskTry <- tasks) yield {
            taskTry match {
              case Left(task) =>
                Some((task.id, new ProjectTask(task.id, task.data, task.metaData, this)))
              case Right(taskLoadingError) =>
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

  /**
   * Defines how tasks are sorted based on their identifier.
   */
  private object TaskOrdering extends Ordering[Identifier] {
    def compare(a:Identifier, b:Identifier): Int = a.toString.compareTo(b.toString)
  }
}