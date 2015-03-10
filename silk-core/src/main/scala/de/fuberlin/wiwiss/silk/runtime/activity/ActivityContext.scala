package de.fuberlin.wiwiss.silk.runtime.activity

import java.util.logging.Logger

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
   * Retrieves the logger to be used by the activity.
   */
  def log: Logger

  /**
   * Executes a child activity and returns after the task has been executed.
   *
   * @param activity The child activity to be executed.
   * @param progressContribution The factor by which the progress of the child activity contributes to the progress of this
   *                             task. A factor of 0.1 means the when the child activity is finished,the progress of the
   *                             parent activity is advanced by 0.1.
   * @param onUpdate A function that is called whenever the value of the child activity has been update.
   * @return The final value of the child activity.
   */
  def executeBlocking[R](activity: Activity[R], progressContribution: Double = 0.0, onUpdate: R => Unit = { _: R => } ): R

  /**
   * Executes a child activity in the background and return immediately.
   *
   * @param activity The child activity to be executed.
   * @param progressContribution The factor by which the progress of the child activity contributes to the progress of this
   *                             task. A factor of 0.1 means the when the child activity is finished,the progress of the
   *                             parent activity is advanced by 0.1.
   * @return An activity control to monitor the progress of the child task. Also allows to cancel the activity.
   */
  def executeBackground[R](activity: Activity[R], progressContribution: Double = 0.0): ActivityControl[R]
}
