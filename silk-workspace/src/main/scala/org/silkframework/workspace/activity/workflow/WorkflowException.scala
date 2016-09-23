package org.silkframework.workspace.activity.workflow

/**
  * Signals that something went wrong during the Workflow execution, which cannot be resolved.
  */
case class WorkflowException(errorMsg: String) extends RuntimeException(errorMsg)
