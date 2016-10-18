package org.silkframework.workspace.activity.workflow

/**
  * Signals that something went wrong during the Workflow execution, which cannot be resolved.
  */
case class WorkflowException(errorMsg: String, cause: Option[Throwable] = None) extends RuntimeException(errorMsg, cause.orNull)
