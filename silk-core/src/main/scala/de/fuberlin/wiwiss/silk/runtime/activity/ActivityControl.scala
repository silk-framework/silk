package de.fuberlin.wiwiss.silk.runtime.activity

/**
 * Holds the current state of the activity.
 */
trait ActivityControl[T] {

  /**
   * The current value of this activity.
   */
  def value: Observable[T]

  /**
   * The current status of this activity.
   */
  def status: Observable[Status]

  /**
   * The running child activity.
   */
  def children(): Seq[ActivityControl[_]]

  /**
   * (Re-)starts this activity.
   * The activity is executed in the background, i.e, the method call returns after the activity has been started.
   *
   * @param activity A new activity that should replace the current one before being started.
   *                 If none, the current activity is restarted.
   * @throws IllegalStateException If the activity is still running.
   */
  def start(activity: Option[Activity[T]] = None): Unit

  /**
   * Starts this activity in the current thread and returns after the activity has been finished.
   *
   * @return The final value of the activity
   */
  def startBlocking(): T

  /**
   * Requests to stop the execution of this activity.
   * There is no guarantee that the activity will stop immediately.
   * Activities need to override cancelExecution() to allow cancellation.
   */
  def cancel()
}
