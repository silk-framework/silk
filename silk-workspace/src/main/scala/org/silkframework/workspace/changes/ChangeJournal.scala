package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.workspace.Project

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
  */
case class ChangeEntry(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], change: Change,
                       reverts: Option[Int] = None)

/**
  * The change journal of a project: every write to the project is recorded as a [[Change]], which can be reverted.
  * Prototype: held in memory only and bounded to the most recent entries.
  */
class ChangeJournal(project: Project) {

  private var entries = Vector.empty[ChangeEntry]

  private var nextSeq = 1

  // Set while an entry is reverted, so the first entry that the revert records refers to it.
  // Not inheritable, so that an activity started by the write does not pick it up.
  private val reverting = new ThreadLocal[Option[Int]] {
    override def initialValue: Option[Int] = None
  }

  // Set while a write is derived from a change that is recorded itself, so that it is not recorded.
  private val derivedWrite = new ThreadLocal[Boolean] {
    override def initialValue: Boolean = false
  }

  /** All entries, oldest first. */
  def all: Seq[ChangeEntry] = synchronized(entries)

  def entry(seq: Int): Option[ChangeEntry] = synchronized(entries.find(_.seq == seq))

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
  private[workspace] def record(change: Change)(implicit userContext: UserContext): Option[ChangeEntry] = synchronized {
    if(derivedWrite.get()) {
      None
    } else {
      val entry = ChangeEntry(nextSeq, Instant.now, userContext.user.map(_.uri), userContext.executionContext.origin,
        change, reverting.get())
      // A change that writes more than one task records one entry per task; only the first one reverts the entry.
      reverting.remove()
      nextSeq += 1
      entries = (entries :+ entry).takeRight(ChangeJournal.maxEntries)
      Some(entry)
    }
  }

  /**
    * Reverts an entry by applying its inverse through the regular write path. The entry this records refers to
    * the reverted one; reverting that entry in turn redoes the change.
    *
    * @throws org.silkframework.runtime.validation.NotFoundException If there is no entry with this sequence number.
    * @throws ChangeConflictException If the entry has been reverted already, has no inverse, or the project changed
    *                                 since so that the inverse does not apply.
    */
  def revert(seq: Int)(implicit userContext: UserContext): ChangeEntry = {
    if(derivedWrite.get()) {
      throw new IllegalStateException(s"Change $seq cannot be reverted from within a derived write.")
    }
    val entry = this.entry(seq).getOrElse(throw new NotFoundException(s"No change $seq in project '${project.id}'."))
    if(revertOf(seq).isDefined) {
      throw ChangeConflictException(s"Change $seq in project '${project.id}' has been reverted already.")
    }
    val inverse = entry.change.inverse.getOrElse(
      throw ChangeConflictException(s"Change $seq (${entry.change.describe}) in project '${project.id}' cannot be reverted."))
    reverting.set(Some(seq))
    try {
      // A revert is a request of the user, so the files its inverse writes are recorded.
      ChangeJournal.onBehalfOf(userContext)(inverse.applyTo(project))
    } finally {
      reverting.remove()
    }
    revertOf(seq).getOrElse(throw new IllegalStateException(s"Reverting change $seq did not record a change."))
  }

  private def revertOf(seq: Int): Option[ChangeEntry] = synchronized(entries.find(_.reverts.contains(seq)))
}

object ChangeJournal {

  /** The number of entries kept per project. */
  val maxEntries: Int = 500

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
