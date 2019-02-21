package org.silkframework.workspace

import java.util.logging.{Level, Logger}

import org.silkframework.config.{MetaData, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
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
  private var error: Option[ValidationException] = None

  /**
    * Returns a validation exception if an error occured during task loading.
    */
  def loadingError: Option[ValidationException] = error

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
    task.init()
    cachedTasks += ((name, task))
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
    logger.info(s"Removed task '$taskId' from project ${project.name}")
  }

  private def load()
                  (implicit userContext: UserContext): Unit = synchronized {
    if(cachedTasks == null) {
      try {
        val tasks = provider.readTasksSafe[TaskData](project.name, project.resources)
        val exceptions = ArrayBuffer[Throwable]()
        cachedTasks = TreeMap()(TaskOrdering) ++ {
          (for (taskTry <- tasks) yield {
            taskTry match {
              case Success(task) =>
                Some((task.id, new ProjectTask(task.id, task.data, task.metaData, this)))
              case Failure(ex) =>
                exceptions.append(ex)
                None
            }
          }).flatten
        }
        handleTaskExceptions(exceptions)
      } catch {
        case NonFatal(ex) =>
          handleUnexpectedException(ex)
      }
    }
  }

  private def handleUnexpectedException(ex: Throwable): Unit = {
    cachedTasks = TreeMap()(TaskOrdering)
    error = Some(new ValidationException(s"Error loading tasks of type ${taskType.getName}. Details: ${ex.getMessage}", ex))
    logger.log(Level.WARNING, s"Error loading tasks of type ${taskType.getName}", ex)
  }

  private def handleTaskExceptions(exceptions: ArrayBuffer[Throwable]): Unit = {
    def exceptionString(ex: Throwable): String = s"${ex.getClass.getSimpleName} (message: ${ex.getMessage})"
    if (exceptions.nonEmpty) {
      error = if (exceptions.size == 1) {
        val ex = exceptions.head
        Some(new ValidationException(s"Error loading tasks of type ${taskType.getName}. Details: ${exceptionString(ex)}", ex))
      } else {
        Some(new ValidationException(s"There were errors loading ${exceptions.size} tasks of type ${taskType.getName}. " +
            s"Details: ${exceptions.map(exceptionString).mkString(", ")}"))
      }
      for (ex <- exceptions) {
        logger.log(Level.WARNING, s"Error loading tasks of type ${taskType.getName}", ex)
      }
    }
  }

  /**
   * Defines how tasks are sorted based on their identifier.
   */
  private object TaskOrdering extends Ordering[Identifier] {
    def compare(a:Identifier, b:Identifier) = a.toString.compareTo(b.toString)
  }
}
