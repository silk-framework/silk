package de.fuberlin.wiwiss.silk.util.task

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.util.Observable

trait HasStatus extends Observable[TaskStatus] {
  /**
   * The level at which task status changes should be logged.
   * Examples are status updates when the task is started and stopped.
   */
  var statusLogLevel = Level.INFO

  /**
   * The level at which updates to the running status logged.
   * Examples are updates to the current progress or the current status message.
   */
  var progressLogLevel = Level.INFO

  /**
   * The logger used to log status changes.
   */
  protected val logger = Logger.getLogger(getClass.getName)

  /**
   * Holds the current status.
   */
  private var currentStatus: TaskStatus = TaskIdle()

  /**
   * The current status of this task.
   */
  def status = currentStatus

  /**
   * Updates the status.
   *
   * @param status The new status
   */
  protected def updateStatus(status: TaskStatus) {
    status match {
      case _: TaskRunning => logger.log(progressLogLevel, status.toString)
      case _ => logger.log(statusLogLevel, status.toString)
    }

    if(!currentStatus.isInstanceOf[TaskCanceling] || status.isInstanceOf[TaskFinished]) {
      currentStatus = status
      publish(status)
    }
  }

  /**
   * Updates the status message.
   *
   * @param status The new status message
   */
  protected def updateStatus(message: String) {
    updateStatus(TaskRunning(message, currentStatus.progress))
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(progress: Double) {
    updateStatus(TaskRunning(currentStatus.message, progress))
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(message: String, progress: Double) {
    updateStatus(TaskRunning(message, progress))
  }
}