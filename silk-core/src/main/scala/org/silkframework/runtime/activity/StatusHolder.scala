package org.silkframework.runtime.activity

import java.util.logging.{Level, Logger}

import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.Status.Canceling

import scala.util.Try

/**
 * Holds the current status of an activity.
 */
class StatusHolder(log: Logger = Logger.getLogger(getClass.getName),
                   parent: Option[StatusHolder] = None,
                   progressContribution: Double = 0.0,
                   val projectAndTaskId: Option[ProjectAndTaskIds] = None) extends Observable[Status] {

  /**
   * The level at which task status changes should be logged.
   * Examples are status updates when the task is started and stopped.
   */
  private val statusLogLevel = Level.INFO

  /**
    * The level at which the failure of a task is logged.
    */
  private val failureLogLevel = Level.WARNING

  /**
   * Holds the current status.
   */
  @volatile
  private var status: Status = Status.Idle()

  /**
   * Retrieves the current status.
   */
  override def apply(): Status = status

  def projectAndTaskIdString: String = projectAndTaskId.map(p => s" $p ").getOrElse("")

  /**
    * True, if canceling has been requested.
    */
  def isCanceling: Boolean = status.isInstanceOf[Canceling]

  /**
   * Updates the current status.
   */
  def update(newStatus: Status, logStatus: Boolean = true) {
    // Log new status change if requested
    if(logStatus) {
      val message = newStatus.toString + " " + projectAndTaskId.getOrElse("")
      newStatus match {
        case _: Status.Running => log.log(StatusHolder.progressLogLevel, message)
        case s: Status.Finished if s.failed => log.log(failureLogLevel, message, s.exception.get)
        case _ => log.log(statusLogLevel, message)
      }
    }

    // Advance the progress of the parent task
    for(p <- parent if progressContribution != 0.0) {
      val progressDiff = newStatus.progress.getOrElse(0.0) - status.progress.getOrElse(0.0)
      p.update(Status.Running(newStatus.message, Some(p.status.progress.getOrElse(0.0) + progressDiff * progressContribution)), logStatus = false)
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
    update(Status.Running(status.message, Some(progress)), logStatus)
  }

  /**
    * Increases the progress.
    *
    * @param increase The amount by which the progress should be increased
    */
  def increaseProgress(increase: Double, logStatus: Boolean = true): Unit = {
    val updatedStatus = synchronized {
      Status.Running(status.message, Some(status.progress.getOrElse(0.0) + increase))
    }
    update(updatedStatus, logStatus)
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def update(message: String, progress: Double) {
    update(Status.Running(message, Some(progress)))
  }
}

object StatusHolder {
  val log: Logger = Logger.getLogger(getClass.getName)
  /**
    * The level at which updates to the running status logged.
    * Examples are updates to the current progress or the current status message.
    */
  lazy val progressLogLevel: Level = {
    val cfg = DefaultConfig.instance()
    val progressLogKey = "logging.di.activity.progress"
    if(cfg.hasPath(progressLogKey)) {
      cfg.getAnyRef(progressLogKey) match {
        case level: String =>
          Try(Level.parse(level)).getOrElse {
            log.warning(s"Invalid log level '$level' for parameter '$progressLogKey'!")
            Level.INFO
          }
        case _ =>
          log.warning(s"Config parameter '$progressLogLevel' must have a string value!")
          Level.INFO
      }
    } else {
      Level.INFO
    }
  }
}

case class ProjectAndTaskIds(projectId: String, taskId: Option[String]) {
  override def toString: String = {
    taskId match {
      case Some(id) =>
        s"(project: $projectId, task: $id)"
      case None =>
        s"(project: $projectId)"
    }
  }
}