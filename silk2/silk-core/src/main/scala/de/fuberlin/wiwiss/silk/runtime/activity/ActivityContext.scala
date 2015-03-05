package de.fuberlin.wiwiss.silk.runtime.activity

/**
 * Holds the context in which a activity is executed.
 * Called to publish updates to the state of the activity and to execute child activities.
 */
trait ActivityContext {

  /**
   * Retrieves current status of the activity.
   */
  def status: Status

  /**
   * Updates the status of the activity.
   */
  def updateStatus(status: Status)

  /**
   * Updates the status message.
   *
   * @param message The new status message
   */
  def updateStatus(message: String) {
    updateStatus(Status.Running(message, status.progress))
  }

  /**
   * Updates the progress.
   *
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def updateStatus(progress: Double) {
    updateStatus(Status.Running(status.message, progress))
  }

  /**
   * Updates the status.
   *
   * @param message The new status message
   * @param progress The progress of the computation (A value between 0.0 and 1.0 inclusive).
   */
  def updateStatus(message: String, progress: Double) {
    updateStatus(Status.Running(message, progress))
  }

  /**
   * Executes a child activity and returns after the task has been executed.
   *
   * @param task The child activity to be executed.
   * @param progressContribution The factor by which the progress of the child activity contributes to the progress of this
   *                             task. A factor of 0.1 means the when the child activity is finished,the progress of the
   *                             parent activity is advanced by 0.1.
   */
  def executeBlocking(task: Activity, progressContribution: Double = 0.0): Unit

  /**
   * Executes a child activity in the background and return immediately.
   *
   * @param task The child activity to be executed.
   * @param progressContribution The factor by which the progress of the child activity contributes to the progress of this
   *                             task. A factor of 0.1 means the when the child activity is finished,the progress of the
   *                             parent activity is advanced by 0.1.
   * @return An activity control to monitor the progress of the child task. Also allows to cancel the activity.
   */
  def executeBackground(task: Activity, progressContribution: Double = 0.0): ActivityControl
}
