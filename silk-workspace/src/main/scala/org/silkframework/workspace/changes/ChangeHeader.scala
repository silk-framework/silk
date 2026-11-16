package org.silkframework.workspace.changes

import org.silkframework.util.Identifier

import java.time.Instant
import scala.util.control.NonFatal

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
  * @param conversationId The companion conversation responsible for this change, if known.
  */
case class ChangeHeader(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], changeType: String,
                        summary: String, details: Seq[ChangeDetail], revertible: Boolean, proposal: Boolean,
                        taskId: Option[Identifier], ruleId: Option[Identifier], path: Option[String], executionId: Option[String],
                        reverts: Option[Int] = None, fulfils: Option[Int] = None, conversationId: Option[String] = None) {

  /** Whether the change came in through a client that names itself, e.g. an MCP agent; these queue for user review. */
  def agentWrite: Boolean = origin.isDefined

  /** The kind of change, as the changes page tags it: added, updated, removed or run. */
  def kind: String = ChangeHeader.kind(changeType, summary)

  /** One line for display, the summary with the details, see [[Change.describe]]. */
  def describe: String = Change.describe(summary, details)
}

object ChangeHeader {

  /**
    * The kind of a change by its type name, e.g. an `AddMapping` adds, a `ResourceDeleted` removes, a `WorkflowExecuted` and
    * its proposal are runs; a variable or tag set for the first time is an addition, which only the summary tells.
    */
  def kind(changeType: String, summary: String): String = {
    if(changeType == "WorkflowExecuted" || changeType == "ProposedWorkflowRun") {
      "run"
    } else if(changeType.startsWith("Add") || changeType == "ResourceCreated" ||
      ((changeType == "SetVariable" || changeType == "SetTag") && summary.startsWith("Added "))) {
      "added"
    } else if(changeType.startsWith("Remove") || changeType.startsWith("Discarded") || changeType == "ResourceDeleted" ||
      changeType == "DisconnectWorkflowNodes") {
      "removed"
    } else {
      "updated"
    }
  }

  /** The header of a new entry, derived from its change; see [[ChangeEntry.apply]] for the parameters. */
  def of(seq: Int, timestamp: Instant, user: Option[String], origin: Option[String], change: Change,
         reverts: Option[Int], fulfils: Option[Int], conversationId: Option[String] = None): ChangeHeader = {
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
    // Display data rendered from arbitrary parameter values: a failure to render them must not fail the write being
    // recorded, so the listing shows the failure where the details would be
    val details = try {
      change.details
    } catch {
      case NonFatal(ex) => Seq(ChangeDetail(s"The details could not be rendered: ${Change.reason(ex)}"))
    }
    ChangeHeader(seq, timestamp, user, origin, change.changeType, change.summary, details,
      change.inverse.isDefined, change.isInstanceOf[Proposal], taskId, ruleId, path, executionId, reverts, fulfils, conversationId)
  }
}
