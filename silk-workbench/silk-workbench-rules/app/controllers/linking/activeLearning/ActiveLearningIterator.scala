package controllers.linking.activeLearning

import models.linking.LinkCandidateDecision
import org.silkframework.entity.Link
import org.silkframework.learning.active.comparisons.ComparisonPairGenerator
import org.silkframework.learning.active.{ActiveLearning, LinkCandidate}
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.workspace.ProjectTask

import java.util.logging.Logger

object ActiveLearningIterator {

  private val log = Logger.getLogger(getClass.getName)

  /**
    *  Adds a new reference link and starts the next active learning iteration.
    *
    *  @param linkSource source URI of the current link candidate
    *  @param linkTarget target URI of the current link candidate
    *  @param decision The decision for the link candidate. One of LinkCandidateDecision.
    *  @param task The project task
    *  @param synchronous If false, the active learning will be run in the background and the call only blocks and waits if no more link candidates are available.
    *                     If true, a new active learning is started in every iteration. This is slower, but deterministic.
    *
    */
  def commitLink(linkSource: String, linkTarget: String, decision: String, task: ProjectTask[LinkSpec], synchronous: Boolean = false)
                (implicit userContext: UserContext): Link = {
    val activity = task.activity[ActiveLearning]
    // Find the link in the reference data, since those have entities attached
    val linkCandidate = activity.value().referenceData.findLink(linkSource, linkTarget) match {
      case Some(l) => l
      case None =>
        throw BadUserInputException(s"Could not find the committed link ('$linkSource' -> '$linkTarget') in either the reference links or the link candidate pool")
    }

    decision match {
      case LinkCandidateDecision.positive =>
        activity.updateValue(activity.value().copy(referenceData = activity.value().referenceData.withPositiveLink(linkCandidate)))
      case LinkCandidateDecision.negative =>
        activity.updateValue(activity.value().copy(referenceData = activity.value().referenceData.withNegativeLink(linkCandidate)))
      case LinkCandidateDecision.unlabeled =>
        activity.updateValue(activity.value().copy(referenceData = activity.value().referenceData.withoutLink(linkCandidate)))
    }

    // Start a learning task
    var finished = !activity.status().isRunning
    if (synchronous) {
      activity.startBlocking()
      finished = true
    } else if (finished) {
      activity.start()
    }

    linkCandidate
  }

  def nextLinkCandidate(task: ProjectTask[LinkSpec])
                       (implicit userContext: UserContext): LinkCandidate = {
    val comparisonActivity = task.activity[ComparisonPairGenerator]
    val activeLearn = task.activity[ActiveLearning]

    // Run active learning initially, if required
    if(comparisonActivity.value().selectedPairs != activeLearn.value().comparisonPaths) {
      activeLearn.control.startBlocking()
    }

    // Pick the next link candidate
    val links = activeLearn.value().links
    if (links.isEmpty) {
      if(activeLearn.status().isRunning) {
        activeLearn.control.waitUntilFinished()
      } else {
        activeLearn.control.startBlocking()
      }
    }

    activeLearn.value().links match {
      case head +: tail =>
        activeLearn.updateValue(activeLearn.value().copy(links = tail))
        head
      case _ =>
        throw NotFoundException("No more link candidates available.")
    }
  }

}
