package org.silkframework.runtime.activity

import org.silkframework.runtime.users.User
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class SimpleUserContextTest extends AnyFlatSpec with Matchers {
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
