package org.silkframework.runtime.activity

import org.silkframework.runtime.activity.Status.Finished

/**
 * Holds the current state of the activity.
 */
trait ActivityControl[T] {

  /**
   * The name of the activity.
   */
  def name: String

  /**
   * The current value of this activity.
   */
  def value: Observable[T]

  /**
   * The current status of this activity.
   */
  def status: Observable[Status]

  /**
   * The running child activities.
   */
  def children(): Seq[ActivityControl[_]]

  /**
   * (Re-)starts this activity.
   * The activity is executed in the background, i.e, the method call returns after the activity has been started.
   *
   * @throws IllegalStateException If the activity is still running.
   */
  def start()(implicit user: UserContext = UserContext.Empty): Unit

  /**
   * Starts this activity in the current thread and returns after the activity has been finished.
   */
  def startBlocking()(implicit user: UserContext = UserContext.Empty): Unit

  /**
   * Starts this activity in the current thread and returns the final value after the activity has been finished.
   *
   * @return The final value of the activity
   */
  def startBlockingAndGetValue(initialValue: Option[T] = None)(implicit user: UserContext = UserContext.Empty): T

  /**
   * Requests to stop the execution of this activity.
   * There is no guarantee that the activity will stop immediately.
   * Activities need to override cancelExecution() to allow cancellation.
   */
  def cancel()

  /**
   * Resets the value of this activity to its initial value.
   */
  def reset()

  /**
    * Returns the underlying activity.
    */
  def underlying: Activity[T]

  def waitUntilFinished() = {
    while(!status().isInstanceOf[Finished]) {
      Thread.sleep(100)
    }
  }
}
