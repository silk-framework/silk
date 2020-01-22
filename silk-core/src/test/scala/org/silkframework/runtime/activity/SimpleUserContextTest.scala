package org.silkframework.runtime.activity

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.runtime.users.User

class SimpleUserContextTest extends FlatSpec with MustMatchers {
  behavior of "Simple user context"

  it should "update its execution context" in {
    val user = new User {
      override def uri: String = "http://user"
    }
    val simpleUserContext = SimpleUserContext(Some(user))
    val updatedContext = simpleUserContext.withExecutionContext(UserExecutionContext(insideWorkflow = true))
    updatedContext.executionContext.insideWorkflow mustBe true
  }
}
