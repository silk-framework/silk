package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

/**
  * A workflow was run. Not revertible; it tells the reviewer what the changes before it were consumed by.
  * It also consumes an open [[ProposedWorkflowRun]] of the task, if one exists.
  *
  * @param executionId The identifier of the persisted execution report, if reports are persisted.
  * @param taskLabel   The workflow's label at run time; None when no label was set.
  */
case class WorkflowExecuted(taskId: Identifier, executionId: Option[String], failed: Boolean,
                            taskLabel: Option[String] = None) extends RecordedChange {

  override def describe: String = s"Executed workflow '${taskLabel.getOrElse(taskId.toString)}'" + (if(failed) ", which failed" else "")

  override def inverse: Option[Change] = None
}

/**
  * An agent proposed to run the workflow: recorded instead of executing, so the run — which would
  * consume the changes before it — happens only after the user has reviewed up to this entry.
  * Reverting the proposal discards it; a later run of the task consumes it
  * ([[ChangeJournal.openRunProposal]]).
  */
case class ProposedWorkflowRun(taskId: Identifier, taskLabel: Option[String] = None) extends Change {

  override def describe: String = s"Proposed to run workflow '${taskLabel.getOrElse(taskId.toString)}'"

  override def inverse: Option[Change] = Some(DiscardedWorkflowRun(taskId, taskLabel))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    throw new IllegalStateException("A proposed workflow run is not applied as a change; the run executes on the agent's retry.")
  }
}

/** Discards a proposed workflow run. Recorded by reverting the proposal; it only records itself, as the proposal changed nothing. */
case class DiscardedWorkflowRun(taskId: Identifier, taskLabel: Option[String] = None) extends Change {

  override def describe: String = s"Discarded the proposed run of workflow '${taskLabel.getOrElse(taskId.toString)}'"

  override def inverse: Option[Change] = None

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    project.changeJournal.record(this)
  }
}
