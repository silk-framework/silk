package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, CurrentTaskStatusListener}
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentPool, CurrentValidationLinks, CurrentPopulation, CurrentActiveLearningTask}

class LearnControl extends DynamicButton {

  override protected val dontCacheRendering = true

  override def render = {
    label = if(CurrentPopulation().isEmpty) "Start" else "Done"
    super.render
  }

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    }
    else if(label == "Done") {
      CurrentActiveLearningTask().cancel()
      JS.Redirect("/population.html")
    }
    else {
      if (!CurrentActiveLearningTask().status.isRunning) {
        val sampleLinksTask =
          new ActiveLearningTask(
            sources = User().project.sourceModule.tasks.map(_.source),
            linkSpec = User().linkingTask.linkSpec,
            paths = User().linkingTask.cache.entityDescs.map(_.paths),
            referenceEntities = User().linkingTask.cache.entities,
            pool = CurrentPool(),
            population = CurrentPopulation()
          )

        CurrentActiveLearningTask() = sampleLinksTask
        sampleLinksTask.runInBackground()
      } else {
        CurrentActiveLearningTask().cancel()
      }

      label = "Done"

      JS.Empty
    }
  }

  /**
   * Listens to changes of the current active learning task.
   */
  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskFinished => partialUpdate {
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
        }
        case _ =>
      }
    }
  }
}