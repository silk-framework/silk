package org.silkframework.runtime.activity

import java.time.Instant

import scala.concurrent.Future

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
   * Holds the timestamp when the activity has been added to the waiting queue.
   * None, if the activity has not been queued before.
   */
  def queueTime: Option[Instant] = None

  /**
    * Holds the timestamp when the activity has started execution.
    * None, if the activity has not been started before.
    */
  def startTime: Option[Instant] = None

  /**
    * The user that started the activity.
    * Refers to the empty user until the activity has been started the first time.
    */
  def startedBy: UserContext

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
  def start()(implicit user: UserContext): Unit

  /** Cancels the currently running activity if necessary and starts it again. */
  def restart()(implicit user: UserContext): Future[Unit]

  /**
   * Starts this activity in the current thread and returns after the activity has been finished.
   */
  def startBlocking()(implicit user: UserContext): Unit

  /**
   * Starts this activity in the current thread and returns the final value after the activity has been finished.
   *
   * @return The final value of the activity
   */
  def startBlockingAndGetValue(initialValue: Option[T] = None)(implicit user: UserContext): T

  /**
    * Starts this activity immediately.
    * If the activity has already been started, but is not being executed yet, it will skip the waiting queue.
    * Prioritized activities will not take a slot in the fork join pool.
    */
  def startPrioritized()(implicit user: UserContext): Unit

  /**
   * Requests to stop the execution of this activity.
   * There is no guarantee that the activity will stop immediately.
   * Activities need to override cancelExecution() to allow cancellation.
   * Calls cancelExecution() on child activities recursively
   */
  def cancel()(implicit user: UserContext)

  /**
   * Resets the value of this activity to its initial value.
   */
  def reset()(implicit userContext: UserContext)

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
  def lastResult: Option[ActivityExecutionResult[T]] = lastCompletedResult

  @volatile
  private var lastCompletedResult: Option[ActivityExecutionResult[T]] = None

  protected def lastResult_=(result: ActivityExecutionResult[T]): Unit = {
    lastCompletedResult = Some(result)
  }
}

case class ActivityExecutionResult[+T](metaData: ActivityExecutionMetaData, resultValue: Option[T])