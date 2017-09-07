package org.silkframework.runtime.validation

import java.net.HttpURLConnection

/**
  * Super class for all more specific 'not found' exceptions. This will automatically lead to a 404 response if thrown inside
  * a controller.
  */
class NotFoundException(msg: String, ex: Option[Throwable] = None) extends ClientRequestException(msg, None, HttpURLConnection.HTTP_NOT_FOUND, "Not Found")

object NotFoundException {

  def apply(msg: String, ex: Option[Throwable] = None): NotFoundException = new NotFoundException(msg, ex)

  def apply(ex: Throwable): NotFoundException = NotFoundException(ex.getMessage, Some(ex))

}
