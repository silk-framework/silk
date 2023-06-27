package org.silkframework.runtime.activity

import org.silkframework.config.SilkVocab
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.users.{DefaultUserManager, User}

/**
  * User context that should be propagated to all actions involving creating, modifying, deleting, executing or querying
  * resources, tasks etc. Used, among other things, for access control, provenance and logging.
  */
trait UserContext {
  def user: Option[User]

  def logInfo: String = user.map(u => s" User: ${u.logInfo}.").getOrElse("")

  def executionContext: UserExecutionContext

  def withExecutionContext(userExecutionContext: UserExecutionContext): UserContext
}

/** Holds information about the (current) execution context. This might get adapted via the withExecutionContext if from there on
  * specific execution parameters are known and can be set.
  * @param insideWorkflow After entering a workflow execution this flag is set to true.
  */
case class UserExecutionContext(insideWorkflow: Boolean = false)

object UserContext {
  val Empty: UserContext = empty(UserExecutionContext())

  /** User context that returns no user.
    * This should be used where no user context makes sense, is not available or for tests. */
  def empty(userExecutionContext: UserExecutionContext): UserContext = SimpleUserContext(None, userExecutionContext)

  // A user that can be used at places where there is no user input and no real user context is needed
  val INTERNAL_USER = SimpleUserContext(Some(DefaultUserManager.get(SilkVocab.internalUser))) // FIXME: Remove necessity for this context

}

case class SimpleUserContext(user: Option[User], executionContext: UserExecutionContext = UserExecutionContext()) extends UserContext {
  override def withExecutionContext(userExecutionContext: UserExecutionContext): UserContext = this.copy(executionContext = userExecutionContext)
}