package org.silkframework.runtime.validation

import java.net.HttpURLConnection

/**
  * This should be thrown if the user has given invalid input. This will lead to 400 errors if thrown inside a REST endpoint.
  */
case class BadUserInputException(msg: String, cause: Option[Throwable] = None)
  extends ClientRequestException(msg, cause, HttpURLConnection.HTTP_BAD_REQUEST, "Bad Request")

object BadUserInputException {

  def apply(ex: Throwable): BadUserInputException = BadUserInputException(ex.getMessage, Some(ex))
  
}