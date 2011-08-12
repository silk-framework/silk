package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.lift.util.DynamicButton
import de.fuberlin.wiwiss.silk.learning.LearningTask
import de.fuberlin.wiwiss.silk.util.task.{Finished, Started, Status}
import collection.mutable.{Subscriber, Publisher}

/**
 * Button to control the learning process.
 */
class LearningControl extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() {
    //TODO check if cache is being loaded

    if (!CurrentLearningTask().isRunning)
      startNewTask()
    else
      CurrentLearningTask().cancel()
  }

  /**
   * Starts a new learning task.
   */
  private def startNewTask() {
    val task = new LearningTask(User().linkingTask.cache.instances)
    CurrentLearningTask() = task
    task.subscribe(LearningTaskListener)
    task.runInBackground()
  }

  /**
   * Listens to changes of the current learning task.
   */
  object LearningTaskListener extends Subscriber[Status, Publisher[Status]] {
    override def notify(pub: Publisher[Status], status: Status) {
      status match {
        case _: Started => label = "Stop"
        case _: Finished => label = "Start"
        case _ =>
      }
    }
  }
}