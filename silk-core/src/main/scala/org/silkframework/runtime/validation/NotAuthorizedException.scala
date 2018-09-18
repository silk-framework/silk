package org.silkframework.runtime.validation

import java.net.HttpURLConnection

/**
  * This will lead to a 401 result if thrown inside a REST endpoint.
  */
case class NotAuthorizedException(msg: String, cause: Option[Throwable] = None) extends RequestException(msg, cause) {

  override val errorTitle: String = "Not authorized"

  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_UNAUTHORIZED)
}

object NotAuthorizedException {

  def apply(ex: Throwable): NotAuthorizedException = NotAuthorizedException(ex.getMessage, Some(ex))

}