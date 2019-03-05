package org.silkframework.runtime.activity

import org.silkframework.config.SilkVocab
import org.silkframework.runtime.users.{DefaultUserManager, User}

/**
  * User context that should be propagated to all actions involving creating, modifying, deleting, executing or querying
  * resources, tasks etc. Used, among other things, for access control, provenance and logging.
  */
trait UserContext {
  def user: Option[User]

  def logInfo: String = user.map(u => "User: " + u.logInfo).getOrElse("")
}

object UserContext {

  /** User context that returns no user.
    * This should be used where no user context makes sense, is not available or for tests. */
  object Empty extends UserContext {
    def user: Option[User] = None
  }

  // A user that can be used at places where there is no user input and no real user context is needed
  val INTERNAL_USER = SimpleUserContext(Some(DefaultUserManager.get(SilkVocab.internalUser))) // FIXME: Remove necessity for this context

}

case class SimpleUserContext(user: Option[User]) extends UserContext