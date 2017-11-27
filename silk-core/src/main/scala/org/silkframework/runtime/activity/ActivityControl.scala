package org.silkframework.runtime.activity

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
    * Holds the timestamp when the activity has been started.
    * Is None if the activity is not running at the moment.
    */
  def startTime: Option[Long] = None

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
   * Calls cancelExecution() on child activities recursively
   */
  def cancel()(implicit user: UserContext = UserContext.Empty)

  /**
   * Resets the value of this activity to its initial value.
   */
  def reset()

  /**
    * Returns the underlying activity.
    */
  def underlying: Activity[T]

  /**
    * Waits until the activity finished execution. Throws an Exception if the activity execution failed.
    */
  def waitUntilFinished(): Unit

  /**
    * Returns the last execution result with execution meta data. Is replaced as soon as an execution finishes successfully
    * or with error.
    */
  def lastResult: Option[ActivityExecutionResult] = lastCompletedResult

  @volatile
  private var lastCompletedResult: Option[ActivityExecutionResult] = None

  case class ActivityExecutionResult(metaData: ActivityExecutionMetaData, resultValue: Option[T])

  protected def lastResult_=(result: ActivityExecutionResult): Unit = {
    lastCompletedResult = Some(result)
  }
}
