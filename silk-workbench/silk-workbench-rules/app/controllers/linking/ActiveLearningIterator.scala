package controllers.linking

import java.util.logging.Logger

import models.linking.LinkCandidateDecision
import org.silkframework.entity.{Link, MinimalLink}
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workbench.Context
import org.silkframework.workspace.ProjectTask

object ActiveLearningIterator {

  private val log = Logger.getLogger(getClass.getName)

  /**
    * Iterates the active learning and selects the next link candidate
    *
    * @param decision The decision for the link candidate. One of [[LinkCandidateDecision]].
    * @param linkSource source URI of the current link candidate
    * @param linkTarget target URI of the current link candidate
    */
  def nextActiveLearnCandidate(decision: String, linkSource: String, linkTarget: String, task: ProjectTask[LinkSpec])
                              (implicit userContext: UserContext): Option[Link] = {
    val activeLearn = task.activity[ActiveLearning].control
    // Try to find the chosen link candidate in the pool, because the pool links have entities attached
    val linkCandidate = activeLearn.value().pool.links.find(l => l.source == linkSource && l.target == linkTarget) match {
      case Some(l) => l
      case None => new MinimalLink(linkSource, linkTarget)
    }

    // Commit link candidate
    decision match {
      case LinkCandidateDecision.positive =>
        task.update(task.data.copy(referenceLinks = task.data.referenceLinks.withPositive(linkCandidate)))
      case LinkCandidateDecision.negative =>
        task.update(task.data.copy(referenceLinks = task.data.referenceLinks.withNegative(linkCandidate)))
      case LinkCandidateDecision.pass =>
    }

    // Assert that a learning task is running
    val finished = !activeLearn.status().isRunning
    if(finished) {
      activeLearn.start()
    }

    // Pick the next link candidate
    val links = activeLearn.value().links

    // Update unlabeled reference links
    if(task.data.referenceLinks.unlabeled.isEmpty) {
      val updatedReferenceLinks = task.data.referenceLinks.copy(unlabeled = activeLearn.value().pool.links.toSet)
      task.update(task.data.copy(referenceLinks = updatedReferenceLinks))
    }

    if(links.isEmpty) {
      log.info("Selecting link candidate: No previous candidates available, waiting until learning task is finished.")
      activeLearn.waitUntilFinished()
      activeLearn.value().links.headOption
    } else if(finished) {
      log.info("Selecting link candidate: A learning task finished, thus selecting its top link candidate (if it hasn't been selected just before).")
      links.find(_ != linkCandidate)
    } else if(links.last == linkCandidate) {
      log.info("Selecting link candidate: No remaining link candidates in current learning task, waiting for the next task to finish.")
      activeLearn.waitUntilFinished()
      activeLearn.value().links.headOption
    } else {
      val currentIndex = links.indexOf(linkCandidate)
      log.info(s"Selecting link candidate: Learning task still running, thus selecting next candidate with index ${currentIndex + 1} from list.")
      Some(links(currentIndex + 1))
    }
  }

}
