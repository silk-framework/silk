package de.fuberlin.wiwiss.silk.runtime.activity

import java.util.logging.{Logger, Level}

/**
 * Holds the current status of an activity.
 */
class StatusHolder(log: Logger, parent: Option[StatusHolder], progressContribution: Double = 0.0) extends Observable[Status] {

  /**
   * The level at which task status changes should be logged.
   * Examples are status updates when the task is started and stopped.
   */
  private var statusLogLevel = Level.INFO

  /**
   * The level at which updates to the running status logged.
   * Examples are updates to the current progress or the current status message.
   */
  private var progressLogLevel = Level.INFO

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
  def update(s: Status) {
    // Log status change
    status match {
      case _: Status.Running => log.log(progressLogLevel, status.toString)
      case _ => log.log(statusLogLevel, status.toString)
    }

    // Advance the progress of the parent task
    for(p <- parent) {
      val progressDiff = status.progress - this.status.progress
      p.update(p.status.progress + progressDiff * progressContribution)
    }

    // Publish status change
    if(!status.isInstanceOf[Status.Canceling]) {
      status = status
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