package org.silkframework.workspace.changes

import org.silkframework.util.Identifier

/**
  * A workflow was run. Not revertible; it tells the reviewer what the changes before it were consumed by.
  *
  * @param executionId The identifier of the persisted execution report, if reports are persisted.
  * @param taskLabel   The workflow's label at run time; None when no label was set.
  */
case class WorkflowExecuted(taskId: Identifier, executionId: Option[String], failed: Boolean,
                            taskLabel: Option[String] = None) extends RecordedChange {

  override def describe: String = s"Executed workflow '${taskLabel.getOrElse(taskId.toString)}'" + (if(failed) ", which failed" else "")

  override def inverse: Option[Change] = None
}
