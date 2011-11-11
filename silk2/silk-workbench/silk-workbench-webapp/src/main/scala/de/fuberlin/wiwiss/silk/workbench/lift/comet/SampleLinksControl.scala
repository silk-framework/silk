package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, CurrentTaskStatusListener}
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentPool, CurrentValidationLinks, CurrentPopulation, CurrentSampleLinksTask}

class SampleLinksControl extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    if (!CurrentSampleLinksTask().status.isRunning) {
      val sampleLinksTask =
        new ActiveLearningTask(
          sources = User().project.sourceModule.tasks.map(_.source),
          linkSpec = User().linkingTask.linkSpec,
          paths = User().linkingTask.cache.entityDescs.map(_.paths),
          referenceEntities = User().linkingTask.cache.entities,
          pool = CurrentPool(),
          population = CurrentPopulation()
        )

      CurrentSampleLinksTask() = sampleLinksTask
      sampleLinksTask.runInBackground()
    } else {
      CurrentSampleLinksTask().cancel()
    }

    JS.Empty
  }

  /**
   * Listens to changes of the current sample task.
   */
  private val sampleTaskListener = new CurrentTaskStatusListener(CurrentSampleLinksTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => label = "Stop"
        case _: TaskFinished => partialUpdate {
          label = "Start"
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
        }
        case _ =>
      }
    }
  }
}