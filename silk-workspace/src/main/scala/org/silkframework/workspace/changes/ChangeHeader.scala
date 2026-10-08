package org.silkframework.workspace.changes

import org.silkframework.util.Identifier

import java.time.Instant

/**
  * A recorded change without the change itself: what the journal derives its state from and what a listing shows. A
  * store keeps the headers of a project in memory and reads an entry's change on demand.
  *
  * @param seq         The sequence number of the entry, unique and increasing within the project's journal.
  * @param timestamp   When the change was recorded.
  * @param user        The URI of the user who made the change, if known.
  * @param origin      The client the change came from, e.g. "mcp:<client name>", if known.
  * @param changeType  The kind of change, e.g. "AddMapping".
  * @param summary     The change without its details, see [[Change.summary]].
  * @param details     What the change changed in detail, see [[Change.details]].
  * @param revertible  Whether the change has an inverse.
  * @param proposal    Whether the change is a [[Proposal]].
  * @param taskId      The task the change concerns, if it concerns one.
  * @param ruleId      The mapping rule the change concerns, if it concerns one: the added or updated rule, the parent
  *                    of a removal or reorder.
  * @param path        The file the change concerns, if it concerns one.
  * @param executionId The identifier of the execution report of a workflow run, if the run has one.
  * @param reverts     The seq of the entry this change reverts, if it was recorded by reverting one.
  * @param fulfils     The seq of the proposal this change fulfilled, if any.
  */
case class ChangeHeader(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], changeType: String,
                        summary: String, details: Seq[ChangeDetail], revertible: Boolean, proposal: Boolean,
                        taskId: Option[Identifier], ruleId: Option[Identifier], path: Option[String], executionId: Option[String],
                        reverts: Option[Int] = None, fulfils: Option[Int] = None) {

  /** Whether the change came in through a client that names itself, e.g. an MCP agent; these queue for user review. */
  def agentWrite: Boolean = origin.isDefined

  /** One line for display, the summary with the details, see [[Change.describe]]. */
  def describe: String = Change.describe(summary, details)
}

object ChangeHeader {

  /** The header of a new entry, derived from its change; see [[ChangeEntry.apply]] for the parameters. */
  def of(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], change: Change,
         reverts: Option[Int], fulfils: Option[Int]): ChangeHeader = {
    val taskId = change match {
      case names: NamesTask => Some(names.taskId)
      case _ => None
    }
    val ruleId = change match {
      case added: AddMapping => Some(added.rule.id)
      case updated: UpdateMapping => Some(updated.after.id)
      case removed: RemoveMapping => Some(removed.parentId)
      case reordered: ReorderMappings => Some(reordered.parentId)
      case _ => None
    }
    val path = change match {
      case ResourceCreated(path, _) => Some(path)
      case ResourceOverwritten(path, _, _) => Some(path)
      case ResourceDeleted(path, _) => Some(path)
      case _ => None
    }
    val executionId = change match {
      case WorkflowExecuted(_, executionId, _, _) => executionId
      case _ => None
    }
    ChangeHeader(seq, timestamp, user, origin, change.changeType, change.summary, change.details,
      change.inverse.isDefined, change.isInstanceOf[Proposal], taskId, ruleId, path, executionId, reverts, fulfils)
  }
}
