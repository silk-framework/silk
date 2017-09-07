package org.silkframework.runtime.validation

/**
  * Client request exception.
  * This will lead to a specified error response if thrown inside a REST endpoint.
  */
class ClientRequestException(msg: String, cause: Option[Throwable], val errorCode: Int, val errorText: String) extends RuntimeException(msg, cause.orNull)
