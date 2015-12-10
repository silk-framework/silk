package org.silkframework.runtime.activity

import java.net.URLEncoder
import java.util.logging.{Logger, Level}

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
  def update(newStatus: Status) {
    // Log status change if there is no parent that will log it in the end
    if(parent.isEmpty) {
      newStatus match {
        case s: Status.Running => log.log(progressLogLevel, s.toString)
        case s => log.log(statusLogLevel, s.toString)
      }
    }

    // Advance the progress of the parent task
    for(p <- parent if progressContribution != 0.0) {
      val progressDiff = newStatus.progress - status.progress
      p.update(newStatus.message, p.status.progress + progressDiff * progressContribution)
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
  def update(message: String) {
    update(Status.Running(message, status.progress))
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def update(progress: Double) {
    update(Status.Running(status.message, progress))
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