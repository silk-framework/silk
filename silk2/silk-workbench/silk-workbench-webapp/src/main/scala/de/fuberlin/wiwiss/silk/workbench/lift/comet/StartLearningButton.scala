package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.util.task.{Finished, Started, Status}
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentStatusListener
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.StartLearningDialog
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}

/**
 * Button to control the learning process.
 */
class StartLearningButton extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    //TODO check if cache is being loaded

    if (!CurrentLearningTask().status.isRunning)
      StartLearningDialog.openCmd
    else {
      CurrentLearningTask().cancel()
      JS.Empty
    }
  }

  /**
   * Listens to changes of the current learning task.
   */
  private val learningTaskListener = new CurrentStatusListener(CurrentLearningTask) {
    override def onUpdate(status: Status) {
      status match {
        case _: Started => label = "Stop"
        case _: Finished => label = "Start"
        case _ =>
      }
    }
  }
}