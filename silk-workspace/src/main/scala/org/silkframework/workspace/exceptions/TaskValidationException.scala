package org.silkframework.workspace.exceptions

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
  * Thrown if a task is invalid.
  */
class TaskValidationException(errorMsg: String) extends RequestException(errorMsg, None) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Task Validation Error"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}
