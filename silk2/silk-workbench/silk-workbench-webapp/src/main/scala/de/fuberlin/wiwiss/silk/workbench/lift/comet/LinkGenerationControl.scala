package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.DynamicButton
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton, TaskControl}
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.GenerateLinksDialog
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentStatusListener, User, UserData}
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.util.task.{Finished, Started, Status}

class LinkGenerationControl extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    if (!CurrentGenerateLinksTask().status.isRunning)
      GenerateLinksDialog.openCmd
    else {
      CurrentGenerateLinksTask().cancel()
      JS.Empty
    }
  }

  /**
   * Listens to changes of the current learning task.
   */
  private val generateLinksTaskListener = new CurrentStatusListener(CurrentGenerateLinksTask) {
    override def onUpdate(status: Status) {
      status match {
        case _: Started => label = "Stop"
        case _: Finished => label = "Start"
        case _ =>
      }
    }
  }
}