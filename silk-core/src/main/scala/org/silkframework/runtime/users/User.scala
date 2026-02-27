package org.silkframework.runtime.users

import org.silkframework.util.Uri

trait User {

  /** The URI of the user */
  def uri: String

  /** The groups this user belongs to */
  def groups: Set[String]

  /** The action URIs granted to this user via OAuth/ACL. */
  def actions: UserActions

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

/** The set of action URIs (e.g. "urn:eccenca:di") granted to a user via OAuth/ACL.
  * @param uris           the action URI strings the user is allowed to perform
  * @param hasAllActions  true if the user holds the AllActions permission, granting unrestricted access
  */
case class UserActions(uris: Set[String] = Set.empty, hasAllActions: Boolean = false) {
  def contains(actionUri: String): Boolean = hasAllActions || uris.contains(actionUri)
}

object UserActions {
  val empty: UserActions = UserActions()
  val all: UserActions = UserActions(hasAllActions = true)
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
