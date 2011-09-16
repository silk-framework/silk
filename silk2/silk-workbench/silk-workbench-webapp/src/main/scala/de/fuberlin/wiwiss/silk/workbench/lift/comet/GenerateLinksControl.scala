package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.util.DynamicButton
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton, TaskControl}
import de.fuberlin.wiwiss.silk.workbench.lift.snippet.GenerateLinksDialog
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskStatusListener, User, UserData}
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}

class GenerateLinksControl extends DynamicButton {

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
  private val generateLinksTaskListener = new CurrentTaskStatusListener(CurrentGenerateLinksTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => label = "Stop"
        case _: TaskFinished => label = "Start"
        case _ =>
      }
    }
  }
}