package org.silkframework.runtime.users

/**
  * A user of the Silk web interface.
  */
class WebUser(val uri: String, val name: Option[String], val groups: Set[String] = Set.empty) extends User {
  /** A unique ID for the request this user has initiated. */
  def requestId: Option[String] = None

  override lazy val label: String = name.getOrElse(User.labelFromUri(uri))
}