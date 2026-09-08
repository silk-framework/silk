package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.{NotFoundException, ValidationException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import java.io.IOException
import java.time.Instant

/**
  * A recorded change.
  *
  * @param seq       The sequence number of this entry, unique and increasing within the project's journal.
  * @param timestamp When the change was recorded.
  * @param user      The URI of the user who made the change, if known.
  * @param origin    The client the change came from, e.g. "mcp:<client name>", if known.
  * @param change    The change that was applied.
  * @param reverts   The seq of the entry this change reverts, if it was recorded by reverting one.
  * @param fulfils   The seq of the proposal this change fulfilled, if it answered an open one, e.g. the run of a proposed workflow run.
  */
case class ChangeEntry(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], change: Change,
                       reverts: Option[Int] = None, fulfils: Option[Int] = None) {

  /** Whether the change came in through a client that names itself, e.g. an MCP agent; these queue for user review. */
  def agentWrite: Boolean = origin.isDefined
}

/**
  * The change journal of a project: every write to the project is recorded as a [[Change]], which can be reverted.
  * The entries are held in the configured [[ChangeJournalStore]].
  */
class ChangeJournal(project: Project) {

  // Set while an entry is reverted, so the first entry that the revert records refers to it.
  // Not inheritable, so that an activity started by the write does not pick it up.
  private val reverting = new ThreadLocal[Option[Int]] {
    override def initialValue: Option[Int] = None
  }

  // Set while a write is derived from a change that is recorded itself, so that it is not recorded.
  private val derivedWrite = new ThreadLocal[Boolean] {
    override def initialValue: Boolean = false
  }

  // Resolved per call, so a config reload swaps the store.
  private def store: ChangeJournalStore = ChangeJournalStore()

  /** All entries, oldest first. */
  def all: Seq[ChangeEntry] = store.entries(project.id)

  def entry(seq: Int): Option[ChangeEntry] = all.find(_.seq == seq)

  /** The seq up to which the user has reviewed the changes; 0 if never set. */
  def reviewedUpTo: Int = store.reviewedUpTo(project.id)

  /** The agent entries after the reviewed watermark, oldest first. The user's own writes do not queue for review,
    * a reverted entry needs no review anymore: its effect is undone, and neither does a run that fulfils an approved
    * proposal: the approval was its review. */
  def unreviewed: Seq[ChangeEntry] = {
    val entries = all
    val watermark = reviewedUpTo
    val reverted = revertedBy(entries)
    entries.filter { entry =>
      entry.seq > watermark && entry.agentWrite && !reverted.contains(entry.seq) && !entry.fulfils.exists(_ <= watermark)
    }
  }

  /** The seq of the entry that reverted each reverted entry. */
  def revertedBy: Map[Int, Int] = revertedBy(all)

  private def revertedBy(entries: Seq[ChangeEntry]): Map[Int, Int] = {
    entries.flatMap(entry => entry.reverts.map(_ -> entry.seq)).toMap
  }

  /** The seq of the entry that fulfilled each fulfilled proposal. A fulfilled proposal is final: it cannot be discarded anymore. */
  def fulfilledBy: Map[Int, Int] = fulfilledBy(all)

  private def fulfilledBy(entries: Seq[ChangeEntry]): Map[Int, Int] = {
    entries.flatMap(entry => entry.fulfils.map(_ -> entry.seq)).toMap
  }

  /** The proposals that are neither discarded nor fulfilled, oldest first, with their entries. */
  private def openProposals(entries: Seq[ChangeEntry]): Seq[(ChangeEntry, Proposal)] = {
    val reverted = revertedBy(entries)
    val fulfilled = fulfilledBy(entries)
    entries.flatMap { entry =>
      entry.change match {
        case proposal: Proposal if !reverted.contains(entry.seq) && !fulfilled.contains(entry.seq) => Some(entry -> proposal)
        case _ => None
      }
    }
  }

  /**
    * Advances the reviewed watermark. Reviews only add up: a seq at or below the watermark is a no-op.
    *
    * @throws ChangeConflictException If `upTo` lies beyond the latest recorded change, which the caller cannot have seen.
    */
  def markReviewed(upTo: Int): Unit = {
    val currentStore = store
    currentStore.synchronized {
      val latestSeq = currentStore.latestSeq(project.id)
      if(upTo > latestSeq) {
        throw ChangeConflictException(s"Cannot mark the changes of project '${project.id}' as reviewed up to $upTo: " +
          s"the latest change is $latestSeq.")
      }
      if(upTo > currentStore.reviewedUpTo(project.id)) {
        currentStore.setReviewedUpTo(project.id, upTo)
      }
    }
  }

  /** Records a proposed irreversible action, e.g. an agent's workflow run that awaits the user's review. */
  def propose(proposal: Proposal)(implicit userContext: UserContext): ChangeEntry = {
    record(proposal).getOrElse(throw new IllegalStateException("A proposal cannot be recorded from a derived write."))
  }

  /** The open proposal to run the task, if any: proposed, not discarded, and not fulfilled by a run of the task. */
  def openRunProposal(taskId: Identifier): Option[ChangeEntry] = {
    openProposals(all).collect { case (entry, ProposedWorkflowRun(`taskId`, _)) => entry }.lastOption
  }

  /**
    * The open proposal to run the task, or a newly recorded one if there is none. Finding and recording is one
    * step under the store's monitor, so calls that arrive together share a proposal instead of stacking one each.
    */
  def proposeRunIfAbsent(taskId: Identifier, proposal: Proposal)(implicit userContext: UserContext): ChangeEntry = {
    val currentStore = store
    currentStore.synchronized {
      openRunProposal(taskId).getOrElse(propose(proposal))
    }
  }

  /** Drops all entries. Called when the project is deleted, so a later project of the same name starts clean. */
  private[workspace] def clear(): Unit = store.remove(project.id)

  /**
    * Runs writes that are derived from a change recorded on this thread, e.g. the tasks re-resolved after a variable
    * change. They are not recorded, as reverting the change restores them too.
    */
  private[workspace] def derived[T](body: => T): T = {
    val outer = derivedWrite.get()
    derivedWrite.set(true)
    try {
      body
    } finally {
      derivedWrite.set(outer)
    }
  }

  /** Records a change that has been applied. Called by the write path; a derived write is not recorded. */
  private[workspace] def record(change: Change)(implicit userContext: UserContext): Option[ChangeEntry] = {
    if(derivedWrite.get()) {
      None
    } else {
      val currentStore = store
      // The seq comes from the store, under its monitor: a project can have more than one journal while it is
      // reloaded, and its journals share the store.
      // A write may run with provider rights, e.g. as the loading user when access control is on, so the user of
      // the request being served is the one who asked for it.
      val requester = ChangeJournal.requestUserContext.getOrElse(userContext)
      currentStore.synchronized {
        // The change answers the latest open proposal it fulfils, e.g. a run the proposal to run its workflow.
        val fulfils = openProposals(currentStore.entries(project.id)).findLast { case (_, proposal) => change.fulfils(proposal) }
        val entry = ChangeEntry(currentStore.latestSeq(project.id) + 1, Instant.now, requester.user.map(_.uri),
          userContext.executionContext.origin, change, reverting.get(), fulfils.map(_._1.seq))
        // A change that writes more than one task records one entry per task; only the first one reverts the entry.
        reverting.remove()
        currentStore.append(project.id, entry)
        Some(entry)
      }
    }
  }

  /**
    * Reverts an entry by applying its inverse through the regular write path. The entry this records refers to
    * the reverted one; reverting that entry in turn redoes the change.
    *
    * @throws org.silkframework.runtime.validation.NotFoundException If there is no entry with this sequence number.
    * @throws ChangeConflictException If the entry is being or has been reverted already, has no inverse, or the
    *                                 project changed since so that the inverse does not apply.
    * @throws ChangeNotRevertedException If the inverse changed nothing that the journal records, e.g. because it
    *                                    restores a value that a variable template resolves to the same value again.
    */
  def revert(seq: Int)(implicit userContext: UserContext): ChangeEntry = {
    if(derivedWrite.get()) {
      throw new IllegalStateException(s"Change $seq cannot be reverted from within a derived write.")
    }
    val inverse = claimRevert(seq)
    reverting.set(Some(seq))
    try {
      // A revert is a request of the user, so the files its inverse writes are recorded.
      ChangeJournal.onBehalfOf(userContext)(inverse.applyTo(project))
    } finally {
      reverting.remove()
      ChangeJournal.synchronized(ChangeJournal.revertsInProgress -= ((project.id, seq)))
    }
    revertOf(seq).getOrElse(throw ChangeNotRevertedException(s"Change $seq in project '${project.id}' has not been " +
      "reverted: applying its inverse changed nothing that the journal records, so the state it restores is derived, " +
      "e.g. from a variable template."))
  }

  /**
    * Reverts entries newest-first, so that no entry is reverted while a later one still builds on it: an entry
    * that cannot be reverted is skipped, one whose inverse changes nothing stays unchanged, a conflict stops the
    * batch and leaves the remaining entries unattempted.
    * Not transactional: the entries reverted before a conflict stay reverted, as the outcomes report.
    */
  def revertAll(seqs: Seq[Int])(implicit userContext: UserContext): Seq[RevertOutcome] = {
    val outcomes = Seq.newBuilder[RevertOutcome]
    var stopped = false
    // Fixed before the batch: a revert never fulfils a proposal.
    val fulfilled = fulfilledBy
    for(seq <- seqs.distinct.sorted(Ordering[Int].reverse)) {
      if(stopped) {
        outcomes += RevertOutcome.NotAttempted(seq)
      } else {
        entry(seq) match {
          case None =>
            outcomes += RevertOutcome.Skipped(seq, s"No change $seq in project '${project.id}'.")
          case Some(e) if e.change.inverse.isEmpty =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq (${e.change.describe}) cannot be reverted.")
          case Some(_) if revertOf(seq).isDefined =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq has been reverted already.")
          case Some(_) if fulfilled.contains(seq) =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq has been fulfilled by change ${fulfilled(seq)}.")
          case Some(_) =>
            try {
              outcomes += RevertOutcome.Reverted(seq, revert(seq))
            } catch {
              // The project is unchanged, so the older entries can still be reverted.
              case ex: ChangeNotRevertedException =>
                outcomes += RevertOutcome.Unchanged(seq, ex.getMessage)
              // An I/O failure, e.g. a file that cannot be deleted, stops the batch with its outcomes reported.
              case ex @ (_: ChangeConflictException | _: ValidationException | _: NotFoundException | _: IOException) =>
                outcomes += RevertOutcome.Conflict(seq, ex.getMessage)
                stopped = true
            }
        }
      }
    }
    outcomes.result()
  }

  /**
    * Claims an entry for reverting and returns the inverse to apply. The claim is held until the revert is recorded,
    * so that an entry is reverted once even if it is reverted concurrently. The inverse is applied without the lock,
    * as it writes to the project.
    */
  private def claimRevert(seq: Int): Change = ChangeJournal.synchronized {
    val entries = all
    val entry = entries.find(_.seq == seq).getOrElse(throw new NotFoundException(s"No change $seq in project '${project.id}'."))
    if(ChangeJournal.revertsInProgress.contains((project.id, seq)) || revertedBy(entries).contains(seq)) {
      throw ChangeConflictException(s"Change $seq in project '${project.id}' has been reverted already.")
    }
    for(fulfilledBy <- fulfilledBy(entries).get(seq)) {
      throw ChangeConflictException(s"Change $seq in project '${project.id}' has been fulfilled by change $fulfilledBy.")
    }
    val inverse = entry.change.inverse.getOrElse(
      throw ChangeConflictException(s"Change $seq (${entry.change.describe}) in project '${project.id}' cannot be reverted."))
    ChangeJournal.revertsInProgress += ((project.id, seq))
    inverse
  }

  private def revertOf(seq: Int): Option[ChangeEntry] = all.find(_.reverts.contains(seq))
}

/** The outcome of one entry within [[ChangeJournal.revertAll]]. */
sealed trait RevertOutcome {
  def seq: Int
}

object RevertOutcome {

  /** The entry was reverted; `entry` records the revert. */
  case class Reverted(seq: Int, entry: ChangeEntry) extends RevertOutcome

  /** The inverse was applied but changed nothing that the journal records, so the entry stays; the batch continues. */
  case class Unchanged(seq: Int, reason: String) extends RevertOutcome

  /** The entry cannot be reverted (unknown, no inverse, or reverted already); the batch continues. */
  case class Skipped(seq: Int, reason: String) extends RevertOutcome

  /** The revert conflicted; the batch stops, the newer entries stay reverted. */
  case class Conflict(seq: Int, reason: String) extends RevertOutcome

  /** Not attempted, as a newer entry conflicted. */
  case class NotAttempted(seq: Int) extends RevertOutcome
}

object ChangeJournal {

  // The entries whose inverse is being applied, by project: their revert is not recorded yet, so nothing else marks
  // them. Held here, as a project can have more than one journal while it is reloaded.
  private var revertsInProgress = Set.empty[(Identifier, Int)]

  // The user of the request being served, for writes that carry no user context, such as resource writes.
  private val requestUser = new ThreadLocal[Option[UserContext]] {
    override def initialValue: Option[UserContext] = None
  }

  /** Serves a request on behalf of a user, so that its writes that carry no user context are attributed to the user. */
  def onBehalfOf[T](userContext: UserContext)(body: => T): T = {
    val outer = requestUser.get()
    requestUser.set(Some(userContext))
    try {
      body
    } finally {
      requestUser.set(outer)
    }
  }

  /** The user of the request being served, if any; an activity, e.g. a workflow run, serves none. */
  private[workspace] def requestUserContext: Option[UserContext] = requestUser.get()
}
