package de.fuberlin.wiwiss.silk.runtime.activity

import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.concurrent.ExecutionContext

/**
 * An activity is a unit of work that can be executed in the background.
 * Implementing classes need to override the run method.
 */
trait Activity {

  def taskName = getClass.getSimpleName.undoCamelCase

  /**
   * Executes this activity.
   * @param context Holds the context in which the activity is executed.
   */
  def run(context: ActivityContext)

  /**
   *  Can be overridden in implementing classes to allow cancellation of the activity.
   */
  def cancelExecution() { }
}

/**
 * Executes activities.
 */
object Activity {

  /**
   * Executes an activity in the background.
   *
   * @param activity The activity to be executed.
   * @return An [ActivityControl] instance that can be used to monitor the execution status as well as the current value
   *         and allows to request the cancellation of the execution.
   */
  def execute(activity: Activity): ActivityControl = {
    val execution = new ActivityExecution(activity)
    ExecutionContext.global.execute(execution)
    execution
  }

  /**
   * Creates an empty activity control.
   */
  def empty = execute(new Activity { def run(context: ActivityContext) = { } })

}



