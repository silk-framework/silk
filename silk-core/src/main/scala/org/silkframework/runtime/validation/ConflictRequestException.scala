package org.silkframework.runtime.validation

/**
  * Request exception representing a 409.
  */
case class ConflictRequestException(msg: String) extends RequestException(msg, None) {
  override def errorTitle: String = "Conflict"

  override def httpErrorCode: Option[Int] = Some(409)
}
