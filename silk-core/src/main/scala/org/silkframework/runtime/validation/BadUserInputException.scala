package org.silkframework.runtime.validation

import java.net.HttpURLConnection

/**
  * This should be thrown if the user has given invalid input. This will lead to 400 errors if thrown inside a REST endpoint.
  */
case class BadUserInputException(msg: String, cause: Option[Throwable] = None) extends ClientRequestException(msg, cause) {

  /**
    * A short error title, e.g, "Task not found".
    */
  override val errorText: String = "Bad Request"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Int = HttpURLConnection.HTTP_BAD_REQUEST
}

object BadUserInputException {

  def apply(ex: Throwable): BadUserInputException = BadUserInputException(ex.getMessage, Some(ex))
  
}