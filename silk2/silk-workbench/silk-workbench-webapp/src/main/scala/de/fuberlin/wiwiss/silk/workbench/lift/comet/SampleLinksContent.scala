package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Unknown, Incorrect, Generated, Correct}
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentSampleLinksTask}
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentTaskValueListener
import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.learning.sampling.SampleLinksTask

class SampleLinksContent extends Links with RateLinkButtons {

  override protected val showDetails = false

  override protected val showEntities = true

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