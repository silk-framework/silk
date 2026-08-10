package org.silkframework.runtime.resource

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
 * Thrown if a resource operation addresses a resource that is outside the permitted area of a resource manager,
 * or that must not be modified at all.
 */
case class ResourceAccessDeniedException(msg: String) extends RequestException(msg, None) {

  /**
   * The HTTP error code.
   */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_FORBIDDEN)

  /**
   * A short error title.
   */
  override val errorTitle: String = "Resource access denied"

}
