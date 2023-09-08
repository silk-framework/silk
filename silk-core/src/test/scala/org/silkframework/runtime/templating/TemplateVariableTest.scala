package org.silkframework.runtime.templating

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.runtime.validation.BadUserInputException

class TemplateVariableTest extends FlatSpec with Matchers {

  behavior of "TemplateVariable"

  it should "validate variable names" in {
    noException shouldBe thrownBy {
      variableName("name")
      variableName("_name123")
    }
    an[BadUserInputException] should be thrownBy {
      variableName("123name")
      variableName("a-b")
    }
  }

  private def variableName(name: String) = TemplateVariable(name, "test value", None, None, isSensitive = false, "testScope")

}
