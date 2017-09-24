package org.silkframework.workbench.utils

import java.net.HttpURLConnection

import org.silkframework.runtime.validation.RequestException

case class NotAcceptableException(msg: String) extends RequestException(msg, None) {

  /**
    * A short error title.".
    */
  override val errorTitle: String = "Not Acceptable"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_NOT_ACCEPTABLE)
}
