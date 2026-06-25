package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TemplateVariableNameTest extends AnyFlatSpec with Matchers {

  behavior of "TemplateVariableName"

  it should "parse full names" in {
    TemplateVariableName.parse("project.var") shouldBe new TemplateVariableName("var", Seq("project"))
    TemplateVariableName.parse("var") shouldBe new TemplateVariableName("var", Seq.empty)
    TemplateVariableName.parse("a.b.c") shouldBe new TemplateVariableName("c", Seq("a", "b"))
  }

}
