package org.silkframework.runtime.activity


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.users.SimpleUser

class SimpleUserContextTest extends AnyFlatSpec with Matchers {
  behavior of "Simple user context"

  it should "update its execution context" in {
    val user = SimpleUser("http://user")
    val simpleUserContext = SimpleUserContext(Some(user))
    val updatedContext = simpleUserContext.withExecutionContext(UserExecutionContext(insideWorkflow = true))
    updatedContext.executionContext.insideWorkflow mustBe true
  }
}
