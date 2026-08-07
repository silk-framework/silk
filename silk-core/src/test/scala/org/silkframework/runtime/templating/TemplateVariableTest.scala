package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.validation.BadUserInputException

class TemplateVariableTest extends AnyFlatSpec with Matchers {

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

  it should "reject an empty variable name with the same error as any other invalid name" in {
    // Used to throw a raw index exception, which surfaced as a 500 instead of a 400
    an[BadUserInputException] should be thrownBy variableName("")
  }

  private def variableName(name: String) = TemplateVariable(name, "test value", None, None, isSensitive = false, VariableScope("testScope"))

}
