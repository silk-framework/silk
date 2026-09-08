package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

/**
  * A workflow was run. Not revertible; it tells the reviewer what the changes before it were consumed by.
  * It fulfils the open [[ProposedWorkflowRun]] of the task, if there is one.
  *
  * @param executionId The identifier of the persisted execution report, if reports are persisted.
  * @param taskLabel   The workflow's label at run time; None when no label was set.
  */
case class WorkflowExecuted(taskId: Identifier, executionId: Option[String], failed: Boolean,
                            taskLabel: Option[String] = None) extends RecordedChange with NamesTask {

  override def summary: String = s"Executed workflow '$taskName'" + (if(failed) ", which failed" else "")

  override def inverse: Option[Change] = None

  override def fulfils(proposal: Proposal): Boolean = proposal match {
    case ProposedWorkflowRun(proposedTaskId, _) => proposedTaskId == taskId
    case _ => false
  }
}

/**
  * An agent proposed to run the workflow: recorded instead of executing, so the run — which would
  * consume the changes before it — happens only after the user has reviewed up to this entry.
  * Reverting the proposal discards it; the next run of the task fulfils it.
  */
case class ProposedWorkflowRun(taskId: Identifier, taskLabel: Option[String] = None) extends Proposal with NamesTask {

  override def summary: String = s"Proposed to run workflow '$taskName'"

  override def inverse: Option[Change] = Some(DiscardedWorkflowRun(taskId, taskLabel))
}

/** Discards a proposed workflow run. Recorded by reverting the proposal; it only records itself, as the proposal changed nothing. */
case class DiscardedWorkflowRun(taskId: Identifier, taskLabel: Option[String] = None) extends Change with NamesTask {

  override def summary: String = s"Discarded the proposed run of workflow '$taskName'"

  override def inverse: Option[Change] = None

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    project.changeJournal.record(this)
  }
}
