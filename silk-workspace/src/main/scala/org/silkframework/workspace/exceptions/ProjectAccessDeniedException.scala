package org.silkframework.workspace.exceptions

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
 * Thrown if the user tries to access a project that they are not allowed to access.
 */
case class ProjectAccessDeniedException(msg: String) extends RequestException(msg, None) {

  /**
   * The HTTP error code..
   */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_FORBIDDEN)

  /**
   * A short error title.
   */
  override val errorTitle: String = "No Access"

}