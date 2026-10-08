package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project
import org.silkframework.workspace.access.ProjectAccessDeniedException

import java.time.Instant
import scala.util.control.NonFatal

/**
  * A recorded change: its header, which a store keeps in memory, and the change that was applied, which a store reads
  * when the entry is asked for. A new entry is built through the companion, which derives the header from the change;
  * a store that reads an entry back pairs the stored header with the change.
  */
case class ChangeEntry(header: ChangeHeader, change: Change) {

  def seq: Int = header.seq

  def timestamp: Instant = header.timestamp

  def user: Option[String] = header.user

  def origin: Option[String] = header.origin

  def reverts: Option[Int] = header.reverts

  def fulfils: Option[Int] = header.fulfils
}

object ChangeEntry {

  /**
    * A newly recorded change.
    *
    * @param seq       The sequence number of the entry, unique and increasing within the project's journal.
    * @param timestamp When the change was recorded.
    * @param user      The URI of the user who made the change, if known.
    * @param origin    The client the change came from, e.g. "mcp:<client name>", if known.
    * @param change    The change that was applied.
    * @param reverts   The seq of the entry this change reverts, if it was recorded by reverting one.
    * @param fulfils   The seq of the proposal this change fulfilled, if any, e.g. a workflow run fulfils the proposal to
    *                  run that workflow.
    */
  def apply(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], change: Change,
            reverts: Option[Int] = None, fulfils: Option[Int] = None): ChangeEntry = {
    ChangeEntry(ChangeHeader.of(seq, timestamp, user, origin, change, reverts, fulfils), change)
  }
}

/**
  * The change journal of a project: every write to the project is recorded as a [[Change]], which can be reverted.
  * The entries are held in the configured [[ChangeJournalStore]].
  *
  * @param loadingUser The user context the store reads the entries with, as the project loads its tasks.
  */
class ChangeJournal(project: Project, loadingUser: UserContext) {

  // Built once, as task loading does: the store reads an entry's change with it when the journal asks for the entry.
  private implicit lazy val readContext: PluginContext = PluginContext.fromProject(project)(loadingUser)

  // The seq of the entry that this thread is reverting, if any. The method `record` below copies it into the
  // `reverts` field of the first journal entry created during the revert. That links the new entry to the reverted one.
  private val reverting = new ThreadLocal[Option[Int]] {
    override def initialValue: Option[Int] = None
  }

  // True while this thread runs derived writes, see `derived`. Derived writes are not recorded.
  private val derivedWrite = new ThreadLocal[Boolean] {
    override def initialValue: Boolean = false
  }

  // Resolved per call, so a config reload swaps the store.
  private def store: ChangeJournalStore = ChangeJournalStore()

  /** The headers of all entries, oldest first. */
  def all: Seq[ChangeHeader] = store.headers(project.id)

  /** An entry with its change, read from the store on demand; None if the journal holds no entry with this seq. */
  def entry(seq: Int): Option[ChangeEntry] = store.entry(project.id, seq)

  private def header(seq: Int): Option[ChangeHeader] = all.find(_.seq == seq)

  /** The seq up to which no change waits for review: set by a review, moved along by `record` while no agent change
    * waits and the cap has dropped nothing unreviewed; 0 at the start. */
  def reviewedUpTo: Int = store.reviewedUpTo(project.id)

  /** The seq of the newest entry, 0 without entries. From the store, so it counts a newest entry whose stored line
    * cannot be read and which `all` leaves out; `markReviewed` accepts seqs up to this one. */
  def latestSeq: Int = store.latestSeq(project.id)

  /** The headers and the reviewed watermark, read in one step, so that both describe the same journal state. */
  def snapshot: (Seq[ChangeHeader], Int) = {
    val currentStore = store
    currentStore.monitor(project.id).synchronized((currentStore.headers(project.id), currentStore.reviewedUpTo(project.id)))
  }

  /** The entries awaiting review, oldest first: the agent entries after the reviewed watermark.
    * Left out are reverted entries (their effect is undone) and runs that fulfil a reviewed proposal (approving the
    * proposal was the review). */
  def unreviewed: Seq[ChangeHeader] = {
    val (headers, watermark) = snapshot
    unreviewed(headers, watermark)
  }

  def unreviewed(headers: Seq[ChangeHeader], watermark: Int): Seq[ChangeHeader] = {
    val reverted = revertedBy(headers)
    headers.filter { header =>
      header.seq > watermark && header.agentWrite && !reverted.contains(header.seq) && !header.fulfils.exists(_ <= watermark)
    }
  }

  /** How many entries the store's cap has dropped that were never reviewed. Seqs are contiguous and a store drops a
    * prefix, so every seq below the oldest kept entry is gone, and every one of them above the watermark was
    * unreviewed when it went. User writes and reverted entries among them count too: the number bounds what the user
    * has not seen rather than counting agent writes. */
  def droppedUnreviewed(headers: Seq[ChangeHeader], watermark: Int): Int = {
    headers.headOption.fold(0)(oldest => math.max(0, oldest.seq - 1 - watermark))
  }

  /** For each reverted entry, the seq of the entry that reverted it. */
  def revertedBy: Map[Int, Int] = revertedBy(all)

  def revertedBy(headers: Seq[ChangeHeader]): Map[Int, Int] = {
    headers.flatMap(header => header.reverts.map(_ -> header.seq)).toMap
  }

  /** For each fulfilled proposal, the seq of the entry that fulfilled it. A fulfilled proposal is final and cannot be discarded. */
  def fulfilledBy: Map[Int, Int] = fulfilledBy(all)

  def fulfilledBy(headers: Seq[ChangeHeader]): Map[Int, Int] = {
    headers.flatMap(header => header.fulfils.map(_ -> header.seq)).toMap
  }

  /** The headers of the proposals that are neither discarded nor fulfilled, oldest first. */
  private def openProposals(headers: Seq[ChangeHeader]): Seq[ChangeHeader] = {
    val reverted = revertedBy(headers)
    val fulfilled = fulfilledBy(headers)
    headers.filter(header => header.proposal && !reverted.contains(header.seq) && !fulfilled.contains(header.seq))
  }

  /**
    * Advances the reviewed watermark. Reviews only add up: a seq at or below the watermark is a no-op.
    *
    * @throws ChangeConflictException If `upTo` lies beyond the latest recorded change, which the caller cannot have seen.
    */
  def markReviewed(upTo: Int): Unit = {
    val currentStore = store
    currentStore.monitor(project.id).synchronized {
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
  def openRunProposal(taskId: Identifier): Option[ChangeHeader] = {
    openProposals(all).filter(ProposedWorkflowRun.proposesRunOf(_, taskId)).lastOption
  }

  /**
    * The open proposal to run the task, or a newly recorded one if there is none. Finding and recording is one
    * step under the store's monitor, so calls that arrive together share a proposal instead of stacking one each.
    */
  def proposeRunIfAbsent(taskId: Identifier, proposal: Proposal)(implicit userContext: UserContext): ChangeHeader = {
    val currentStore = store
    currentStore.monitor(project.id).synchronized {
      openRunProposal(taskId).getOrElse(propose(proposal).header)
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
      // A write may run as the provider user, e.g. the loading user when access control is on.
      // The entry names the user of the request being served, if there is one.
      val requester = ChangeJournal.requestUserContext.getOrElse(userContext)
      // The seq is taken under the store's monitor for the project.
      // While a project is reloaded it can have two journals, which share the store and its monitor.
      currentStore.monitor(project.id).synchronized {
        val headers = currentStore.headers(project.id)
        // Links the change to the latest open proposal it fulfils, e.g. a workflow run to the proposal to run that workflow.
        val fulfils = openProposals(headers).findLast(change.fulfils)
        val entry = ChangeEntry(currentStore.latestSeq(project.id) + 1, Instant.now, requester.user.map(_.uri),
          userContext.executionContext.origin, change, reverting.get(), fulfils.map(_.seq))
        // A revert can record several entries, one per task it writes. Only the first is marked as the revert.
        reverting.remove()
        currentStore.append(project.id, entry)(PluginContext.fromProject(project)(userContext))
        // While nothing waits for review, the watermark follows the journal, so the dropped count only covers what fell
        // under the cap while agent changes waited. Checked before the cap: a just dropped agent change still blocks it,
        // and once it is gone the dropped count blocks the move, so the count lasts until the next review.
        val watermark = currentStore.reviewedUpTo(project.id)
        if(unreviewed(headers :+ entry.header, watermark).isEmpty && droppedUnreviewed(headers, watermark) == 0) {
          currentStore.setReviewedUpTo(project.id, entry.seq)
        }
        Some(entry)
      }
    }
  }

  /**
    * The reason why each entry cannot be reverted as the project is now, by seq. Entries that can be reverted are absent.
    * Reads each entry's change and asks [[Change.conflict]] of its inverse, with one [[ConflictContext]] shared by all
    * entries. An entry without inverse is absent as well, unless its header says it has one and its stored change
    * cannot be read. Not checked: whether an entry has been reverted or fulfilled already.
    */
  def revertConflicts(headers: Seq[ChangeHeader])(implicit userContext: UserContext): Map[Int, String] = {
    val context = new ConflictContext(project)
    (for(header <- headers; conflict <- revertConflict(header, context)) yield header.seq -> conflict).toMap
  }

  private def revertConflict(header: ChangeHeader, context: ConflictContext)(implicit userContext: UserContext): Option[String] = {
    if(!header.revertible) {
      None
    } else {
      entry(header.seq).flatMap(_.change.inverse) match {
        case Some(inverse) => inverse.conflict(context)
        // The header says there is an inverse, but the stored line cannot be read, as a whole or as a change
        case None => Some(s"Change ${header.seq} in project '${project.id}' cannot be reverted: its stored change cannot be read.")
      }
    }
  }

  /**
    * Reverts an entry by applying its inverse through the regular write path. The entry this records refers to
    * the reverted one; reverting that entry in turn redoes the change.
    *
    * @throws org.silkframework.runtime.validation.NotFoundException If there is no entry with this sequence number.
    * @throws ChangeConflictException If the entry is being or has been reverted already, has no inverse, the project
    *                                 changed since so that the inverse does not apply, or applying it failed, e.g. a
    *                                 workflow that does not validate with the node restored; the failure is the cause.
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
      // File writes are recorded only while a request user is set, so the inverse runs on behalf of the reverting user.
      ChangeJournal.onBehalfOf(userContext)(inverse.applyTo(project))
    } catch {
      // Conflicts and access denials pass through.
      // Any other failure of the inverse becomes a conflict, as the conflict check reports it.
      case ex @ (_: ChangeConflictException | _: ProjectAccessDeniedException) => throw ex
      case NonFatal(ex) => throw ChangeConflictException(Change.reason(ex), Some(ex))
    } finally {
      reverting.remove()
      ChangeJournal.synchronized(ChangeJournal.revertsInProgress -= ((project.id, seq)))
    }
    revertOf(seq).flatMap(header => entry(header.seq)).getOrElse(throw ChangeNotRevertedException(s"Change $seq in project '${project.id}' has not been " +
      "reverted: applying its inverse changed nothing that the journal records, so the state it restores is derived, " +
      "e.g. from a variable template."))
  }

  /**
    * Reverts entries newest first, so that no entry is reverted while a later one still builds on it.
    * An entry that cannot be reverted is skipped. An entry whose inverse changes nothing stays as it is.
    * A conflict stops the batch: the remaining entries are not attempted.
    * Not transactional: the entries reverted before a conflict stay reverted, as the outcomes report.
    */
  def revertAll(seqs: Seq[Int])(implicit userContext: UserContext): Seq[RevertOutcome] = {
    val outcomes = Seq.newBuilder[RevertOutcome]
    var stopped = false
    // Read once for the batch: a revert never fulfils a proposal, so the batch does not change it.
    val fulfilled = fulfilledBy
    for(seq <- seqs.distinct.sorted(Ordering[Int].reverse)) {
      if(stopped) {
        outcomes += RevertOutcome.NotAttempted(seq)
      } else {
        header(seq) match {
          case None =>
            outcomes += RevertOutcome.Skipped(seq, s"No change $seq in project '${project.id}'.")
          case Some(h) if !h.revertible =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq (${h.describe}) cannot be reverted.")
          case Some(_) if revertOf(seq).isDefined =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq has been reverted already.")
          case Some(_) if fulfilled.contains(seq) =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq has been fulfilled by change ${fulfilled(seq)}.")
          // Read once here and again by the revert: an entry whose stored change cannot be read is skipped, not a conflict
          case Some(h) if entry(seq).forall(_.change.inverse.isEmpty) =>
            outcomes += RevertOutcome.Skipped(seq, s"Change $seq (${h.describe}) cannot be reverted: its stored change cannot be read.")
          case Some(_) =>
            try {
              outcomes += RevertOutcome.Reverted(seq, revert(seq))
            } catch {
              // The project is unchanged, so the older entries can still be reverted.
              case ex: ChangeNotRevertedException =>
                outcomes += RevertOutcome.Unchanged(seq, ex.getMessage)
              // The entry was evicted from the store after the check above. Nothing to revert, the batch continues.
              case ex: NotFoundException =>
                outcomes += RevertOutcome.Skipped(seq, ex.getMessage)
              // A conflict, which is how revert reports any failure of the inverse, stops the batch.
              case ex: ChangeConflictException =>
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
  private def claimRevert(seq: Int): Change = {
    // Read before the companion's monitor is taken, which all projects share: the first access loads the journal, and
    // the change is read from the store. An entry's change never changes, so the checks below may follow the read.
    val loaded = entry(seq).getOrElse(throw new NotFoundException(s"No change $seq in project '${project.id}'."))
    ChangeJournal.synchronized {
      val headers = all
      if(ChangeJournal.revertsInProgress.contains((project.id, seq)) || revertedBy(headers).contains(seq)) {
        throw ChangeConflictException(s"Change $seq in project '${project.id}' has been reverted already.")
      }
      for(fulfilledBy <- fulfilledBy(headers).get(seq)) {
        throw ChangeConflictException(s"Change $seq in project '${project.id}' has been fulfilled by change $fulfilledBy.")
      }
      val inverse = loaded.change.inverse.getOrElse(
        throw ChangeConflictException(s"Change $seq (${loaded.change.describe}) in project '${project.id}' cannot be reverted."))
      ChangeJournal.revertsInProgress += ((project.id, seq))
      inverse
    }
  }

  private def revertOf(seq: Int): Option[ChangeHeader] = all.find(_.reverts.contains(seq))
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

  // The reverts in progress, as (project, seq): claimed, but not recorded yet, so the journal does not show them as reverted.
  // Held in the companion, as a project can have two journals while it is reloaded.
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
