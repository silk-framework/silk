package org.silkframework.runtime.validation

/**
  * Exception class to signal that a service is not ready, yet, or has other temporary issues and needs to be initialized first.
  */
case class ServiceUnavailableException(message: String) extends RequestException(message, None) {
  final val serviceUnavailableStatusCode = 503

  override def errorTitle: String = "Service Temporarily Unavailable"

  override def httpErrorCode: Option[Int] = Some(serviceUnavailableStatusCode)
}
