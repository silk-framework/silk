package org.silkframework.runtime.activity

/**
  * Trait that provides a user context for tests.
  */
trait TestUserContextTrait {
  // By default only anonymous access is allowed.
  implicit def userContext: UserContext = UserContext.Empty
}
