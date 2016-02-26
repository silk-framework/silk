package org.silkframework.runtime.activity

import java.util.logging.{Level, Logger}

/**
 * Holds the current status of an activity.
 */
class StatusHolder(log: Logger = Logger.getLogger(getClass.getName),
                   parent: Option[StatusHolder] = None,
                   progressContribution: Double = 0.0) extends Observable[Status] {

  /**
   * The level at which task status changes should be logged.
   * Examples are status updates when the task is started and stopped.
   */
  private val statusLogLevel = Level.INFO

  /**
   * The level at which updates to the running status logged.
   * Examples are updates to the current progress or the current status message.
   */
  private val progressLogLevel = Level.INFO

  /**
    * The level at which the failure of a task is logged.
    */
  private val failureLogLevel = Level.WARNING

  /**
   * Holds the current status.
   */
  @volatile
  private var status: Status = Status.Idle

  /**
   * Retrieves the current status.
   */
  override def apply(): Status = status

  /**
   * Updates the current status.
   */
  def update(newStatus: Status, logStatus: Boolean = true) {
    // Log new status change if requested
    if(logStatus) {
      newStatus match {
        case s: Status.Running => log.log(progressLogLevel, s.toString)
        case s: Status.Finished if s.failed => log.log(failureLogLevel, s.toString)
        case s => log.log(statusLogLevel, s.toString)
      }
    }

    // Advance the progress of the parent task
    for(p <- parent if progressContribution != 0.0) {
      val progressDiff = newStatus.progress - status.progress
      p.update(Status.Running(newStatus.message, p.status.progress + progressDiff * progressContribution), logStatus = false)
    }

    // Publish status change
    // If this task is in Canceling state, it doesn't accept new status changes except the finished status.
    if(!status.isInstanceOf[Status.Canceling] || !newStatus.isRunning) {
      status = newStatus
      publish(status)
    }
  }

  /**
   * Updates the status message.
   *
   * @param message The new status message
   */
  def updateMessage(message: String, logStatus: Boolean = true) {
    update(Status.Running(message, status.progress), logStatus)
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def updateProgress(progress: Double, logStatus: Boolean = true) {
    update(Status.Running(status.message, progress), logStatus)
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def update(message: String, progress: Double) {
    update(Status.Running(message, progress))
  }
}