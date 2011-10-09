package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Unknown, Incorrect, Generated, Correct}
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink
import de.fuberlin.wiwiss.silk.entity.Link
import de.fuberlin.wiwiss.silk.workbench.learning.CurrentValidationLinks
import de.fuberlin.wiwiss.silk.workbench.workspace.UserDataListener

class SampleLinksContent extends LinksContent with RateLinkButtons {

  override protected val showDetails = false

  override protected val showEntities = true

  override protected val showStatus = false

  private val linkListener = new UserDataListener(CurrentValidationLinks) {
    override def onUpdate(links: Seq[Link]) {
      partialUpdate(updateLinksCmd)
    }
  }

  override protected def links: Seq[EvalLink] = {
    def links = linkingTask.referenceLinks

    for (link <- CurrentValidationLinks().view) yield {
      if (links.positive.contains(link))
        new EvalLink(link, Correct, Generated)
      else if (links.negative.contains(link))
        new EvalLink(link, Incorrect, Generated)
      else
        new EvalLink(link, Unknown, Generated)
    }
  }.sortBy(_.confidence.get.abs)

}