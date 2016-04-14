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
  def progress: Double = 0.0

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
  def succeeded = !isRunning && !failed

  /**
    * The timestamp when the status has been updated.
    */
  val timestamp = System.currentTimeMillis()

  /**
   * The complete status message including the progress (if running).
   */
  override def toString = message
}

object Status {
  /**
   * Status which indicates that the task has not been started yet.
   */
  case class Idle() extends Status {
    def message = "Idle"
  }
  
  /**
   * Status which indicates that the task has been started.
   */
  case class Started() extends Status {
    override def message = "Started"
    override def isRunning = true
  }
  
  /**
   * Running status
   *
   * @param message The status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  case class Running(message: String, override val progress: Double) extends Status {
    override def isRunning = true
    override def toString = {
      if(progress != 0.0)
        message + " (" + "%3.1f".format(progress * 100.0) + "%)"
      else
        message
    }
  }
  
  /**
   * Indicating that the task has been requested to stop but has not stopped yet.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  case class Canceling(override val progress: Double) extends Status {
    override def message = "Stopping..."
    override def isRunning = true
  }
  
  /**
   * Status which indicates that the task has finished execution.
   *
   * @param success True, if the computation finished successfully. False, otherwise.
   * @param runtime The time in milliseconds needed to execute the task.
   * @param exception The exception, if the task failed.
   */
  case class Finished(success: Boolean, runtime: Long, exception: Option[Throwable] = None) extends Status {
    override def message = exception match {
      case None => "Finished in " + formattedTime
      case Some(ex) => "Failed after " + formattedTime + ": " + ex.getMessage
    }

    private def formattedTime = {
      if (runtime < 1000)
        runtime + "ms"
      else
        (runtime / 1000) + "s"
    }
  
    override def progress = 1.0
  
    override def failed = !success
  }
}