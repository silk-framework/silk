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

package org.silkframework.runtime.activity

/**
 * A status message
 */
sealed trait Status {

  /**
    * The name of this status, e.g., Idle.
    */
  def name: String = getClass.getSimpleName

  /**
   * The current status message.
   */
  def message: String

  /**
   * The progress of the computation.
   * Will be 0.0 when the task has been started and 1.0 when it has finished execution.
   */
  def progress: Option[Double] = None

  /**
   * True, if the task is running at the moment; False, otherwise.
   */
  def isRunning: Boolean = false

  /**
   * True, if the task has failed.
   */
  def failed: Boolean = false

  /**
   * True, if the task succeeded.
   */
  def succeeded: Boolean = !isRunning && !failed

  /**
    * Providing an exception if execution has failed
    */
  def exception: Option[Throwable] = None

  /**
    * The timestamp when the status has been updated.
    * Milliseconds since midnight, January 1, 1970 UTC.
    */
  val timestamp: Long = System.currentTimeMillis()

  /**
   * The complete status message including the progress (if running).
   */
  override def toString: String = message
}

object Status {
  /**
   * Status which indicates that the activity has not been started yet.
   */
  case class Idle() extends Status {
    def message: String = "Idle"
  }
  
  /**
   * Status which indicates that the activity has been started and is waiting to be executed.
   */
  case class Waiting() extends Status {
    override def message: String = "Waiting"
    override def isRunning: Boolean = true
  }
  
  /**
   * Running status, activity is currently being executed.
   *
   * @param message The status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  case class Running(message: String, override val progress: Option[Double]) extends Status {
    override def isRunning: Boolean = true
    override def toString: String = {
      progress match {
        case Some(p) =>
          message + " (" + "%3.1f".format(p * 100.0) + "%)"
        case None =>
          message
      }
    }
  }

  object Running {
    def apply(message: String, progress: Double): Running = Running(message, Some(progress))
  }

  /**
   * Indicating that the activity has been requested to stop but has not stopped yet.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  case class Canceling(override val progress: Option[Double]) extends Status {
    override def message: String = "Stopping..."
    override def isRunning: Boolean = true
  }
  
  /**
   * Status which indicates that the activity has finished execution.
   *
   * @param success True, if the computation finished successfully. False, otherwise.
   * @param runtime The time in milliseconds needed to execute the task.
   * @param exception The exception, if the task failed.
   */
  case class Finished(success: Boolean, runtime: Long, cancelled: Boolean, override val exception: Option[Throwable] = None) extends Status {
    override def message: String = (success, exception, cancelled) match {
      case (true, None, false) => "Finished in " + formattedTime
      case (_, _, true) => "Cancelled after " + formattedTime
      case (false, Some(ex), _) => "Failed after " + formattedTime + ": " + ex.getMessage
      case (true, Some(ex), _) => "Errors occurred after " + formattedTime + ": " + ex.getMessage
    }

    private def formattedTime = {
      if (runtime < 1000) {
        runtime + "ms"
      } else {
        "%.3fs".format(runtime.toDouble / 1000)
      }
    }
  
    override def progress: Option[Double] = Some(1.0)
  
    override def failed: Boolean = !success
  }
}
