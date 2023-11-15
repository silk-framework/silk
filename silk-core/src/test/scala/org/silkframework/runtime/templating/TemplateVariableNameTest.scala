package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TemplateVariableNameTest extends AnyFlatSpec with Matchers {

  behavior of "TemplateVariableName"

  it should "parse full names" in {
    TemplateVariableName.parse("project.var") shouldBe new TemplateVariableName("var", "project")
    TemplateVariableName.parse("var") shouldBe new TemplateVariableName("var", "")
    TemplateVariableName.parse("a.b.c") shouldBe new TemplateVariableName("b.c", "a")
  }

}
