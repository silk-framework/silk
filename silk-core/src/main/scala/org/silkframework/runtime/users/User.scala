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

  /**
    * Two users are equal if they share the same URI.
    */
  override def equals(other: Any): Boolean = {
    other match {
      case user: User => uri == user.uri
      case _ => false
    }
  }

  /**
    * Hash code based on the user URI.
    */
  override def hashCode(): Int = {
    uri.hashCode
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
