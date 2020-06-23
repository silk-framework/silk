package org.silkframework.runtime.activity

import java.util.logging.Logger

import scala.reflect.ClassTag

/**
 * Holds the context in which a activity is executed.
 * Called to publish updates to the state of the activity and to execute child activities.
 */
trait ActivityContext[T] {

  /**
   * Holds the current value.
   */
  def value: ValueHolder[T]

  /**
   * Retrieves current status of the activity.
   */
  def status: StatusHolder

  /**
    * Will provide context information relevant for the Activity to be performed (if any)
    * @return - ActivityContextData of the specified type
    */
  def contextObject[C](implicit ct:ClassTag[C]): Option[ActivityContextData[C]] = None

  /**
   * Retrieves the logger to be used by the activity.
   */
  def log: Logger

  /**
   * Adds a child activity.
   *
   * @param activity The child activity to be added.
   * @param progressContribution The factor by which the progress of the child activity contributes to the progress of this
   *                             activity. A factor of 0.1 means the when the child activity is finished,the progress of the
   *                             parent activity is advanced by 0.1.
   * @return The activity control for the child activity.
   */
  def child[R](activity: Activity[R], progressContribution: Double = 0.0): ActivityControl[R]

  /**
    * Blocks execution until a given condition is met.
    * This should be called by Activities whenever they are waiting indefinitely.
    *
    * @param condition Evaluates the condition to wait for. Will be called frequently.
    */
  def blockUntil(condition: () => Boolean)
}
