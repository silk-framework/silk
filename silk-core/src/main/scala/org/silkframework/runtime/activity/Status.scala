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
   * The complete status message including the progress (if running).
   */
  override def toString = message
}

object Status {
  /**
   * Status which indicates that the task has not been started yet.
   */
  object Idle extends Status {
    def message = "Idle"
  }
  
  /**
   * Status which indicates that the task has been started.
   */
  case class Started(name: String) extends Status {
    override def message = name + " started"
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
    override def toString = message + " (" + "%3.1f".format(progress * 100.0) + "%)"
  }
  
  /**
   * Indicating that the task has been requested to stop but has not stopped yet.
   *
   * @param name The name of the task.
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  case class Canceling(name: String, override val progress: Double) extends Status {
    override def message = "Stopping " + name
    override def isRunning = true
  }
  
  /**
   * Status which indicates that the task has finished execution.
   *
   * @param name The name of the task.
   * @param success True, if the computation finished successfully. False, otherwise.
   * @param time The time in milliseconds needed to execute the task.
   * @param exception The exception, if the task failed.
   */
  case class Finished(name: String, success: Boolean, time: Long, exception: Option[Throwable] = None) extends Status {
    override def message = exception match {
      case None => name + " finished in " + formattedTime
      case Some(ex) => name + " failed after " + formattedTime + ": " + ex.getMessage
    }
  
    private def formattedTime = {
      if (time < 1000)
        time + "ms"
      else
        (time / 1000) + "s"
    }
  
    override def progress = 1.0
  
    override def failed = !success
  }
}