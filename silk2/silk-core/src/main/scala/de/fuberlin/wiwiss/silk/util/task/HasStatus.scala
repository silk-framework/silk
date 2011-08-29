package de.fuberlin.wiwiss.silk.util.task

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.util.Observable

trait HasStatus extends Observable[Status] {
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
  private var currentStatus: Status = Idle()

  /**
   * The current status of this task.
   */
  def status = currentStatus

  def failed = currentStatus match {
    case Finished(_, false, _) => true
    case _ => false
  }

  /**
   * Updates the status.
   *
   * @param status The new status
   */
  protected def updateStatus(status: Status) {
    status match {
      case _: Running => logger.log(progressLogLevel, status.toString)
      case _ => logger.log(statusLogLevel, status.toString)
    }

    if(!currentStatus.isInstanceOf[Canceling] || status.isInstanceOf[Finished]) {
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
    updateStatus(Running(message, currentStatus.progress))
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(progress: Double) {
    updateStatus(Running(currentStatus.message, progress))
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  protected def updateStatus(message: String, progress: Double) {
    updateStatus(Running(message, progress))
  }
}