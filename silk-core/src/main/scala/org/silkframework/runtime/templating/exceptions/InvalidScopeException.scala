package org.silkframework.runtime.templating.exceptions

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
  * Thrown if a template variable has an invalid scope.
  *
  */
class InvalidScopeException(msg: String, cause: Option[Exception] = None) extends RequestException(msg, cause) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Invalid template variable scope"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}
