package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.learning.CurrentLearningTask
import de.fuberlin.wiwiss.silk.util.task.{Finished, Started, Status}
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.StartLearningDialog
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, CurrentStatusListener}

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
    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    } else if(User().linkingTask.alignment.positive.size < 1 || User().linkingTask.alignment.negative.size < 1) {
      JS.Message("Positive and negative reference links are needed in order to learn a link specification")
    } else if (!CurrentLearningTask().status.isRunning)
      StartLearningDialog.openCmd
    else {
      CurrentLearningTask().cancel()
      label = "Wait"
      enabled = false
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
        case _: Finished => {
          label = "Start"
          enabled = true
        }
        case _ =>
      }
    }
  }
}