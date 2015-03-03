package de.fuberlin.wiwiss.silk.runtime.task

import java.util.concurrent.{TimeUnit, ThreadPoolExecutor, Executors, Callable}
import java.util.logging.{Logger, Level}

import de.fuberlin.wiwiss.silk.util.Observable
import de.fuberlin.wiwiss.silk.util.StringUtils._

/**
 * A task that can be executed.
 */
trait Task {

  def taskName = getClass.getSimpleName.undoCamelCase

  /**
   * Executes this task.
   * @param context Holds the context in which a task is executed.
   */
  def execute(context: TaskContext)

  /**
   *  Can be overridden in implementing classes to allow cancellation of the task.
   */
  def cancelExecution() { }
}

object Task {
  /**
   * Creates an empty task.
   */
  def empty = new Task { def execute(context: TaskContext) = { } }
}

/**
 * Holds the context in which a task is executed.
 * Called to publish updates to the state of the task and to execute child tasks.
 */
trait TaskContext {

  /**
   * Retrieves current status of the task.
   */
  def status: Status

  /**
   * Updates the status of the task.
   */
  def updateStatus(status: Status)

  /**
   * Updates the status message.
   *
   * @param message The new status message
   */
  def updateStatus(message: String) {
    updateStatus(Status.Running(message, status.progress))
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def updateStatus(progress: Double) {
    updateStatus(Status.Running(status.message, progress))
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def updateStatus(message: String, progress: Double) {
    updateStatus(Status.Running(message, progress))
  }

  /**
   * Executes a child task and returns after the task has been executed.
   *
   * @param task The child task to be executed.
   * @param progressContribution The factor by which the progress of the child task contributes to the progress of this
   *                             task. A factor of 0.1 means the when the child task is finished,the progress of the
   *                             parent task is advanced by 0.1.
   */
  def executeBlocking(task: Task, progressContribution: Double = 0.0): Unit

  /**
   * Executes a child task in the background and return immediately.
   *
   * @param task The child task to be executed.
   * @param progressContribution The factor by which the progress of the child task contributes to the progress of this
   *                             task. A factor of 0.1 means the when the child task is finished,the progress of the
   *                             parent task is advanced by 0.1.
   * @return A task control to monitor the progress of the child task. Also allows to cancel the task.
   */
  def executeBackground(task: Task, progressContribution: Double = 0.0): TaskControl
}

/**
 * Holds the current state of the task.
 */
trait TaskControl extends Observable[Status] {

  /**
   * The current status of this task.
   */
  def status: Status

  /**
   * The running child tasks.
   */
  def children(): Seq[TaskControl]

  /**
   * Requests to stop the execution of this task.
   * There is no guarantee that the task will stop immediately.
   * Tasks need to override cancelExecution() to allow cancellation.
   */
  def cancel()
}