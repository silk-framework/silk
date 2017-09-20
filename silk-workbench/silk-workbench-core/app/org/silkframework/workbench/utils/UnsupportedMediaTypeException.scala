package org.silkframework.workbench.utils

import java.net.HttpURLConnection

import org.silkframework.runtime.validation.ClientRequestException

case class UnsupportedMediaTypeException(msg: String) extends ClientRequestException(msg, None) {

  /**
    * A short error title.".
    */
  override val errorText: String = "Unsupported Media Type"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Int = HttpURLConnection.HTTP_UNSUPPORTED_TYPE
}

object UnsupportedMediaTypeException {

  def supportedFormats(expectedFormats: String*): UnsupportedMediaTypeException = {
    UnsupportedMediaTypeException(
      msg = s"Payload must be provided in one of the following formats: ${expectedFormats.mkString(", ")}. Please set the Content-Type header accordingly."
    )
  }

}
