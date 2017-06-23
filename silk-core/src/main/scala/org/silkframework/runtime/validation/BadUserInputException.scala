package org.silkframework.runtime.validation

/**
  * This should be thrown if the user has given invalid input. This will lead to 400 errors if thrown inside a REST endpoint.
  */
case class BadUserInputException(msg: String) extends IllegalArgumentException(msg)