package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Project

import java.time.Instant
import scala.util.DynamicVariable

/**
  * A recorded change.
  *
  * @param user    The URI of the user who made the change, if known.
  * @param origin  The client the change came from, e.g. "mcp:<client name>", if known.
  * @param reverts The entry this change reverts, if it was recorded by reverting one.
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

  // Set while an entry is reverted, so the entry that the revert records refers to it.
  private val reverting = new DynamicVariable[Option[Int]](None)

  /** All entries, oldest first. */
  def all: Seq[ChangeEntry] = synchronized(entries)

  def entry(seq: Int): Option[ChangeEntry] = synchronized(entries.find(_.seq == seq))

  /** Records a change that has been applied. Called by the write path. */
  private[workspace] def record(change: Change)(implicit userContext: UserContext): ChangeEntry = synchronized {
    val entry = ChangeEntry(nextSeq, Instant.now, userContext.user.map(_.uri), userContext.executionContext.origin,
      change, reverting.value)
    nextSeq += 1
    entries = (entries :+ entry).takeRight(ChangeJournal.maxEntries)
    entry
  }

  /**
    * Reverts an entry by applying its inverse through the regular write path. The entry this records refers to
    * the reverted one; reverting that entry in turn redoes the change.
    *
    * @throws ChangeConflictException If the entry has been reverted already, or the project changed since so that
    *                                 the inverse does not apply.
    */
  def revert(seq: Int)(implicit userContext: UserContext): ChangeEntry = {
    val entry = this.entry(seq).getOrElse(throw new NoSuchElementException(s"No change $seq in project '${project.id}'."))
    if(revertOf(seq).isDefined) {
      throw ChangeConflictException(s"Change $seq in project '${project.id}' has been reverted already.")
    }
    reverting.withValue(Some(seq)) {
      entry.change.inverse.applyTo(project)
    }
    revertOf(seq).getOrElse(throw new IllegalStateException(s"Reverting change $seq did not record a change."))
  }

  private def revertOf(seq: Int): Option[ChangeEntry] = synchronized(entries.find(_.reverts.contains(seq)))
}

object ChangeJournal {

  /** The number of entries kept per project. */
  val maxEntries: Int = 500
}
