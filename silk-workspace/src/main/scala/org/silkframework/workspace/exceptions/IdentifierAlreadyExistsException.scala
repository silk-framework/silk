package org.silkframework.workspace.exceptions

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
  * Thrown if the user tries to add a project/task with the same identifier as an already existing project/task.
  *
  * @param msg The detailed error description.
  */
case class IdentifierAlreadyExistsException(msg: String) extends RequestException(msg, None) {

  /**
    * The HTTP error code..
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_CONFLICT)

  /**
    * A short error title.
    */
  override val errorTitle: String = "Identifier already exists"

}
