package org.silkframework.runtime.validation

import java.net.HttpURLConnection

/**
  * This will lead to a 403 result if thrown inside a REST endpoint.
  */
case class ForbiddenException(msg: String, cause: Option[Throwable] = None) extends RequestException(msg, cause) {

  override val errorTitle: String = "Forbidden"

  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_FORBIDDEN)
}

object ForbiddenException {

  def apply(ex: Throwable): ForbiddenException = ForbiddenException(ex.getMessage, Some(ex))

}