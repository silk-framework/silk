package controllers.linking.activeLearning

import models.linking.LinkCandidateDecision
import org.silkframework.entity.{Link, MinimalLink}
import org.silkframework.learning.LearningException
import org.silkframework.learning.active.{ActiveLearning, LinkCandidate}
import org.silkframework.rule.LinkSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.ProjectTask

import java.util.logging.Logger

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
  //TODO remove this obsolete function
  def iterateOld(decision: String, linkSource: String, linkTarget: String, task: ProjectTask[LinkSpec], synchronous: Boolean = false)
             (implicit userContext: UserContext): Option[LinkCandidate] = {
    val activeLearn = task.activity[ActiveLearning].control

    // Commit link candidate
    val linkCandidate = commitLink(linkSource, linkTarget, decision, task)

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

  def iterate(decision: String, linkSource: String, linkTarget: String, task: ProjectTask[LinkSpec], synchronous: Boolean = false)
                (implicit userContext: UserContext): Option[LinkCandidate] = {
    commitLink(linkSource, linkTarget, decision, task, synchronous)
    Some(nextLinkCandidate(task))
  }

  def commitLink(linkSource: String, linkTarget: String, decision: String, task: ProjectTask[LinkSpec], synchronous: Boolean = false)
                (implicit userContext: UserContext): Link = {
    val activity = task.activity[ActiveLearning]
    // Try to find the chosen link candidate in the pool, because the pool links have entities attached
    val linkCandidate = activity.value().referenceData.linkCandidates.find(l => l.source == linkSource && l.target == linkTarget) match {
      case Some(l) => l
      case None => new MinimalLink(linkSource, linkTarget)
    }

    decision match {
      case LinkCandidateDecision.positive =>
        activity.updateValue(activity.value().copy(referenceData = activity.value().referenceData.withPositiveLink(linkCandidate)))
      case LinkCandidateDecision.negative =>
        activity.updateValue(activity.value().copy(referenceData = activity.value().referenceData.withNegativeLink(linkCandidate)))
      case LinkCandidateDecision.pass =>
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
    val activeLearn = task.activity[ActiveLearning]

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
        throw new LearningException("Could not find any link candidates")
    }
  }

}
