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

package de.fuberlin.wiwiss.silk.runtime.task

import de.fuberlin.wiwiss.silk.runtime.task.Task._
import java.util.logging.Level
import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, Callable, Executors}
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * A task which computes a result.
 * While executing the status of the execution can be queried.
 */
trait Task[+T] extends HasStatus with (() => T) {

  var taskName = getClass.getSimpleName.undoCamelCase

  @volatile private var currentSubTask: Option[Task[_]] = None

  /**
   * Executes this task and returns the result.
   */
  override final def apply(): T = synchronized {
    val startTime = System.currentTimeMillis
    updateStatus(TaskStarted(taskName))

    try {
      val result = execute()
      updateStatus(TaskFinished(taskName, true, System.currentTimeMillis - startTime))
      result
    } catch {
      case ex: Throwable => {
        logger.log(Level.WARNING, taskName + " failed", ex)
        updateStatus(TaskFinished(taskName, false, System.currentTimeMillis - startTime, Some(ex)))
        throw ex
      }
    }
  }

  /**
   * Executes this task in a background thread
   */
  def runInBackground(): Future[T] = {
    Task.backgroundExecutor.submit(toCallable(this))
  }

  /**
   * Requests to stop the execution of this task.
   * There is no guarantee that the task will stop immediately.
   * Subclasses need to override stopExecution() to allow cancellation.
   */
  def cancel() {
    if(status.isRunning && !status.isInstanceOf[TaskCanceling]) {
      updateStatus(TaskCanceling(taskName, status.progress))
      currentSubTask.map(_.cancel())
      stopExecution()
    }
  }

  /**
   *  Must be overridden in subclasses to do the actual computation.
   */
  protected def execute(): T

  /**
   *  Can be overridden in subclasses to allow cancellation of the task.
   */
  protected def stopExecution() { }

  /**
   * Executes a sub task inside this task.
   *
   * @param subTask The sub task to be executed.
   * @param finalProgress
   * @param silent If true, the update messages of the subtask will not be logged.
   */
  protected def executeSubTask[R](subTask: Task[R], finalProgress: Double = 1.0, silent: Boolean = false): R = {
    require(finalProgress >= status.progress, "finalProgress >= progress")

    //Set the current sub task
    currentSubTask = Some(subTask)

    //Disable logging of the subtask as this task will do the logging
    subTask.statusLogLevel = Level.FINEST
    subTask.progressLogLevel = Level.FINEST

    //If silent, disable logging and restore the old settings later.
    val oldStatusLogLevel = statusLogLevel
    val oldProgressLogLevel = progressLogLevel
    if(silent) {
      statusLogLevel = Level.FINEST
      progressLogLevel = Level.FINEST
    }

    //Subscribe to status changes of the sub task
    val listener = new (TaskStatus => Unit) {
      val initialProgress = status.progress

      def apply(status: TaskStatus) {
        status match {
          case TaskRunning(msg, taskProgress) => {
            updateStatus(msg, initialProgress + taskProgress * (finalProgress - initialProgress))
          }
          case TaskFinished(_, success, _, _) if success => {
            updateStatus(finalProgress)
          }
          case _ =>
        }
      }
    }

    //Start sub task
    try {
      subTask.onUpdate(listener)
      subTask()
    } finally {
      currentSubTask = None
      //Restore original logging levels
      statusLogLevel = oldStatusLogLevel
      progressLogLevel = oldProgressLogLevel
    }
  }
}

object Task {
  /**
   * Converts a task to a Java Runnable
   */
  implicit def toRunnable[T](task: Task[T]) = new Runnable {
    override def run() = task.apply()
  }

  /**
   * Converts a task to a Java Callable
   */
  implicit def toCallable[T](task: Task[T]) = new Callable[T] {
    override def call() = task.apply()
  }

  /**
   * Creates a task from a delayed value.
   */
  def apply[T](f: => T) = new Task[T] { def execute() = { f } }

  /**
   * Creates an empty task.
   */
  def empty = new Task[Unit] { def execute() = { } }

  /**
   * The executor service used to execute link specs in the background.
   */
  private val backgroundExecutor = {
    val executor = Executors.newCachedThreadPool()

    //Reducing the keep-alive time of the executor, so it won't prevent the JVM from shutting down to long
    executor.asInstanceOf[ThreadPoolExecutor].setKeepAliveTime(2, TimeUnit.SECONDS)

    executor
  }
}
