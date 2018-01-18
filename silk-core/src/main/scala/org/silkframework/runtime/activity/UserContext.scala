package org.silkframework.runtime.activity

import org.silkframework.runtime.users.User

/**
  * Context in which an activity is executed.
  */
trait UserContext {
  def user: Option[User]
}

object UserContext {

  object Empty extends UserContext {
    def user: Option[User] = None
  }

}

case class SimpleUserContext(user: Option[User]) extends UserContext