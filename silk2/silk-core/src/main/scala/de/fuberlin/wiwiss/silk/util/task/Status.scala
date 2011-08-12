package de.fuberlin.wiwiss.silk.util.task

/**
 * A status message
 */
sealed trait Status {
  /**
   * The current status message.
   */
  def message: String

  /**
   * The progress of the computation.
   * Will be 0.0 when the task has been started and 1.0 when it has finished execution.
   */
  def progress: Double

  /**
   * The complete status message including the progress.
   */
  override def toString = message + " (" + "%3.1f".format(progress * 100.0) + "%)"
}

/**
 * Status which indicates that the task has not been started yet.
 */
case class Idle() extends Status {
  def message = "Idle"

  def progress = 0.0
}

/**
 * Status which indicates that the task has been started.
 */
case class Started(name: String) extends Status {
  def message = name + " started"

  def progress = 0.0
}

/**
 * Status which indicates that the task has finished execution.
 *
 * @param success True, if the computation finished successfully. False, otherwise.
 * @param exception The exception, if the task failed.
 */
case class Finished(name: String, success: Boolean, exception: Option[Exception] = None) extends Status {
  def message = exception match {
    case None => name + " finished"
    case Some(ex) => name + " failed: " + ex.getMessage
  }

  def progress = 1.0
}

/**
 * Running status
 *
 * @param message The status message
 * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
 */
case class Running(message: String, progress: Double) extends Status

case class Canceled(name: String, progress: Double) extends Status {
  def message = "Stopping " + name
}