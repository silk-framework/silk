package org.silkframework.serialization.json

import java.net.HttpURLConnection

import org.silkframework.runtime.validation.RequestException

case class JsonParseException(msg: String, cause: Option[Throwable] = None) extends RequestException(msg, cause) {
  /**
    * A short description of the error type.
    */
  override def errorTitle: String = "Could not parse JSON"

  /**
    * The HTTP error code that fits best to the given error type.
    */
  override def httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)
}
