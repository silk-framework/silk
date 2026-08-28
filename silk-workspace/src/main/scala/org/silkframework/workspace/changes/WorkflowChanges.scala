package org.silkframework.workspace.changes

import org.silkframework.util.Identifier

/**
  * A workflow was run. Not revertible; it tells the reviewer what the changes before it were consumed by.
  *
  * @param executionId The identifier of the persisted execution report, if reports are persisted.
  */
case class WorkflowExecuted(taskId: Identifier, executionId: Option[String], failed: Boolean) extends RecordedChange {

  override def describe: String = s"Executed workflow '$taskId'" + (if(failed) ", which failed" else "")

  override def inverse: Option[Change] = None
}
