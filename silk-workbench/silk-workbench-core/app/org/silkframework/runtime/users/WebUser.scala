package org.silkframework.runtime.users

/**
  * A user of the Silk web interface.
  */
class WebUser(val uri: String, val name: Option[String]) extends User {
  /** A unique ID for the request this user has initiated. */
  def requestId: Option[String] = None
}