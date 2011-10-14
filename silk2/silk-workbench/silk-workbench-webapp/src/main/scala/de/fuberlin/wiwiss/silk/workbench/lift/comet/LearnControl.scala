package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.lift.snippet.StartLearningDialog
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}
import de.fuberlin.wiwiss.silk.workbench.workspace.{UserDataListener, User, CurrentTaskStatusListener}
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentPopulation, CurrentLearningTask}
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.util.task.{TaskRunning, TaskFinished, TaskStarted, TaskStatus}

/**
 * Button to control the learning process.
 */
class LearnControl extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    } else if(User().linkingTask.referenceLinks.positive.size < 1 || User().linkingTask.referenceLinks.negative.size < 1) {
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
  private val learningTaskListener = new CurrentTaskStatusListener(CurrentLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => label = "Stop"
        case _: TaskFinished => {
          label = "Start"
          enabled = true
          partialUpdate{ CurrentPopulation() = task.value.get.population }
        }
        case _: TaskRunning => {
          partialUpdate{ CurrentPopulation() = task.value.get.population }
        }
        case _ =>
      }
    }
  }
}