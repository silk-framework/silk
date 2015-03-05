package de.fuberlin.wiwiss.silk.runtime.activity

import de.fuberlin.wiwiss.silk.runtime.activity.Observable

/**
 * Holds the current state of the activity.
 */
trait ActivityControl extends Observable[Status] {

  /**
   * The current status of this activity.
   */
  def status: Status

  /**
   * The running child activity.
   */
  def children(): Seq[ActivityControl]

  /**
   * Requests to stop the execution of this activity.
   * There is no guarantee that the activity will stop immediately.
   * Activities need to override cancelExecution() to allow cancellation.
   */
  def cancel()
}
