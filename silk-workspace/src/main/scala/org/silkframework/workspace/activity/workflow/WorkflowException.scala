package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.validation.RequestException

/**
  * Signals that something went wrong during the Workflow execution, which cannot be resolved.
  */
case class WorkflowException(errorMsg: String, cause: Option[Throwable] = None) extends RequestException(errorMsg, cause) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Workflow Execution Error"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = {
    cause.collect { case ex: RequestException => ex.httpErrorCode }.flatten
  }
}
