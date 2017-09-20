package org.silkframework.workbench.utils

import java.net.HttpURLConnection

import org.silkframework.runtime.validation.ClientRequestException

case class NotAcceptableException(msg: String) extends ClientRequestException(msg, None) {

  /**
    * A short error title.".
    */
  override val errorText: String = "Not Acceptable"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Int = HttpURLConnection.HTTP_NOT_ACCEPTABLE
}
