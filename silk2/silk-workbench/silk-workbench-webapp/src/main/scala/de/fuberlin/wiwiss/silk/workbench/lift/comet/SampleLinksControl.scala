package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}
import de.fuberlin.wiwiss.silk.workbench.workspace.{User, CurrentTaskStatusListener}
import de.fuberlin.wiwiss.silk.learning.sampling.GenerateSampleTask
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentValidationLinks, CurrentPopulation, CurrentSampleLinksTask}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks

class SampleLinksControl extends DynamicButton {

  override protected val dontCacheRendering = true

  label = "Start"

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {
    if (!CurrentSampleLinksTask().status.isRunning) {
      val sampleLinksTask =
        new GenerateSampleTask(
          sources = User().project.sourceModule.tasks.map(_.source),
          linkSpec = User().linkingTask.linkSpec,
          paths = User().linkingTask.cache.entityDescs.map(_.paths),
          referenceEntities = User().linkingTask.cache.entities,
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
        case _: TaskFinished => {
          label = "Start"
          CurrentPopulation() = task.population
          val links = task.links

          //Categorize links
          val posLinks = links.filter(_.confidence.get > 0.99).take(3)
          val negLinks = links.filter(_.confidence.get < -0.99).take(3)
          val valLinks = links.filter(l => l.confidence.get >= -0.99 && l.confidence.get <= 0.99).take(5)

          //Add new reference links
          val project = User().project
          val linkingTask = User().linkingTask
          val referenceLinks = linkingTask.referenceLinks
          val updatedReferenceLinks = ReferenceLinks(referenceLinks.positive ++ posLinks, referenceLinks.negative ++ negLinks)
          val updatedTask = linkingTask.updateReferenceLinks(updatedReferenceLinks, project)

          project.linkingModule.update(updatedTask)
          User().task = updatedTask

          //Set validation links
          CurrentValidationLinks() = valLinks ++ posLinks ++ negLinks
        }
        case _ =>
      }
    }
  }
}