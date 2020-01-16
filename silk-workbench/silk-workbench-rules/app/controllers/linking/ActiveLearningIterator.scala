package controllers.linking

import java.util.logging.Logger

import models.linking.LinkCandidateDecision
import org.silkframework.entity.{Link, MinimalLink}
import org.silkframework.learning.active.ActiveLearning
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.ProjectTask

object ActiveLearningIterator {

  private val log = Logger.getLogger(getClass.getName)

  /**
    * Iterates the active learning and selects the next link candidate
    *
    * @param decision The decision for the link candidate. One of [[LinkCandidateDecision]].
    * @param linkSource source URI of the current link candidate
    * @param linkTarget target URI of the current link candidate
    * @param task The project task
    * @param synchronous If false, the active learning will be run in the background and the call only blocks and waits if no more link candidates are available.
    *                    If true, a new active learning is started in every iteration. This is slower, but deterministic.
    */
  def nextActiveLearnCandidate(decision: String, linkSource: String, linkTarget: String, task: ProjectTask[LinkSpec], synchronous: Boolean = false)
                              (implicit userContext: UserContext): Option[Link] = {
    val activeLearn = task.activity[ActiveLearning].control
    // Try to find the chosen link candidate in the pool, because the pool links have entities attached
    val linkCandidate = activeLearn.value().pool.links.find(l => l.source == linkSource && l.target == linkTarget) match {
      case Some(l) => l
      case None => new MinimalLink(linkSource, linkTarget)
    }

    // Commit link candidate
    commitLink(linkCandidate, decision, task)

    // Assert that a learning task is running
    var finished = !activeLearn.status().isRunning
    if(synchronous) {
      activeLearn.startBlocking()
      finished = true
    } else if(finished) {
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

  private def commitLink(linkCandidate: Link, decision: String, task: ProjectTask[LinkSpec])(implicit userContext: UserContext): Unit = {
    decision match {
      case LinkCandidateDecision.positive =>
        task.update(task.data.copy(referenceLinks = task.data.referenceLinks.withPositive(linkCandidate)))
      case LinkCandidateDecision.negative =>
        task.update(task.data.copy(referenceLinks = task.data.referenceLinks.withNegative(linkCandidate)))
      case LinkCandidateDecision.pass =>
    }
  }

}
