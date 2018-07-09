package org.silkframework.runtime.activity

import org.silkframework.runtime.users.User

/**
  * User context in which resources and tasks are created, modified, deleted, executed or queried. Used, among other things,
  * for access control, provenance and logging.
  */
trait UserContext {
  def user: Option[User]
}

object UserContext {

  implicit object Empty extends UserContext {
    def user: Option[User] = None
  }

}

case class SimpleUserContext(user: Option[User]) extends UserContext