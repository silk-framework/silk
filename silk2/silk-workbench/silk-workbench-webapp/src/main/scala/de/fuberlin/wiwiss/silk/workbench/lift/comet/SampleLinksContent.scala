package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Unknown, Incorrect, Generated, Correct}
import de.fuberlin.wiwiss.silk.workbench.evaluation.{CurrentGenerateLinksTask, EvalLink}
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskRunning, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.learning.{SampleLinksTask, CurrentSampleLinksTask}
import net.liftweb.http.js.JsCmds.Alert
import de.fuberlin.wiwiss.silk.workbench.workspace.{CurrentTaskValueListener, CurrentTaskStatusListener}
import de.fuberlin.wiwiss.silk.output.Link

class SampleLinksContent extends Links with RateLinkButtons {

  override protected val showDetails = false

  override protected val showInstances = true

  override protected val showStatus = false

  private var sampleLinksTask = CurrentSampleLinksTask()

  private val currentSampleLinksTaskListener = (task: SampleLinksTask) => { sampleLinksTask = task }

  CurrentSampleLinksTask.onUpdate(currentSampleLinksTaskListener)

  private val linkListener = new CurrentTaskValueListener(CurrentSampleLinksTask) {
    override def onUpdate(links: Seq[Link]) {
      partialUpdate(updateLinksCmd)
    }
  }

  override protected def links: Seq[EvalLink] = {
    def links = linkingTask.referenceLinks

    for (link <- CurrentSampleLinksTask().links.view) yield {
      if (links.positive.contains(link)) {
        new EvalLink(link, Correct, Generated)
      } else if (links.negative.contains(link)) {
        new EvalLink(link, Incorrect, Generated)
      } else {
        new EvalLink(link, Unknown, Generated)
      }
    }
  }.sortBy(-_.confidence)

}