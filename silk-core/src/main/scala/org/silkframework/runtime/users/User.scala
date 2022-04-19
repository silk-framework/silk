package org.silkframework.runtime.users

import org.silkframework.util.Uri

trait User {

  /** The URI of the user */
  def uri: String

  /** user information that can appear in logs to debug issues  */
  def logInfo: String = s"User(URI = $uri)"

  /**
    * The name of this user.
    */
  lazy val label: String = {
    User.labelFromUri(new Uri(uri))
  }

}

object User {

  /**
    * Extracts a user label from a user URI
    * */
  def labelFromUri(uri: Uri): String = {
    uri.localName.getOrElse(uri.toString)
  }

  /**
    * Extracts a user label from a user URI
    * */
  def labelFromUri(uri: String): String = {
    labelFromUri(new Uri(uri))
  }

}
