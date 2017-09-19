package org.silkframework.runtime.validation

/**
  * Client request exception.
  * This will lead to a JSON error response if thrown inside a REST endpoint.
  *
  * @param msg The detailed error description.
  * @param cause The optional cause of this exception.
  *
  */
abstract class ClientRequestException(msg: String, cause: Option[Throwable]) extends RuntimeException(msg, cause.orNull) {

  /**
    * A short description of the error type, e.g, "Task not found".
    * Should be the same for all instances of the error type.
    */
  def errorText: String

  /**
    * The HTTP error code that fits best to the given error type. Typically in the 4xx range.
    */
  def httpErrorCode: Int

}
