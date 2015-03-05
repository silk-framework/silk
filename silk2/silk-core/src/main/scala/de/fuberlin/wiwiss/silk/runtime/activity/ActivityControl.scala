package de.fuberlin.wiwiss.silk.runtime.activity

import de.fuberlin.wiwiss.silk.runtime.activity.Observable

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
   * Requests to stop the execution of this activity.
   * There is no guarantee that the activity will stop immediately.
   * Activities need to override cancelExecution() to allow cancellation.
   */
  def cancel()
}
