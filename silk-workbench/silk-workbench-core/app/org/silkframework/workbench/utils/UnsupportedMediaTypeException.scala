package org.silkframework.workbench.utils

import java.net.HttpURLConnection

import org.silkframework.runtime.validation.RequestException

case class UnsupportedMediaTypeException(msg: String) extends RequestException(msg, None) {

  /**
    * A short error title.".
    */
  override val errorTitle: String = "Unsupported Media Type"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_UNSUPPORTED_TYPE)
}

object UnsupportedMediaTypeException {

  def supportedFormats(expectedFormats: String*): UnsupportedMediaTypeException = {
    UnsupportedMediaTypeException(
      msg = s"Payload must be provided in one of the following formats: ${expectedFormats.mkString(", ")}. Please set the Content-Type header accordingly."
    )
  }

}
