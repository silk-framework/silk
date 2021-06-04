package org.silkframework.workspace.activity.workflow

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

case class WorkflowValidationException(errorMsg: String) extends RequestException(errorMsg, None) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Illegal Workflow Nesting"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}
