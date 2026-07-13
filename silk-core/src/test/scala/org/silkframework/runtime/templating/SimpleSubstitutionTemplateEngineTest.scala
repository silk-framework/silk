package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.templating.exceptions.UnboundVariablesException

import java.io.StringWriter

class SimpleSubstitutionTemplateEngineTest extends AnyFlatSpec with Matchers {

  behavior of "SimpleSubstitutionTemplateEngine"

  private val engine = SimpleSubstitutionTemplateEngine()

  it should "substitute scoped and unscoped variable references" in {
    evaluate("{{name}} ({{project.year}}-{{ project.month }})",
      new TemplateVariableValue("name", VariableScope.empty, Seq("Terminator")),
      new TemplateVariableValue("year", VariableScope("project"), Seq("2002")),
      new TemplateVariableValue("month", VariableScope("project"), Seq("June"))
    ) shouldBe "Terminator (2002-June)"
  }

  it should "report referenced variables" in {
    val template = engine.compile("{{execution.a}} and {{b}} and {{execution.a}}")
    template.variables.get.map(_.scopedName) shouldBe Seq("execution.a", "b")
  }

  it should "throw an UnboundVariablesException for unbound variables" in {
    val ex = the[UnboundVariablesException] thrownBy {
      evaluate("{{execution.missing}}")
    }
    ex.missingVars.map(_.scopedName) shouldBe Seq("execution.missing")
  }

  it should "evaluate unbound variables to their name if configured" in {
    val writer = new StringWriter()
    engine.compile("-{{execution.missing}}-").evaluate(Seq.empty, writer, EvaluationConfig(ignoreUnboundVariables = true))
    writer.toString shouldBe "-execution.missing-"
  }

  it should "leave text without variable references unchanged" in {
    evaluate("No variables {here} at all") shouldBe "No variables {here} at all"
  }

  it should "resolve values from nested maps" in {
    val writer = new StringWriter()
    val values = Map[String, AnyRef](
      "name" -> "Terminator",
      "project" -> new java.util.HashMap[String, AnyRef](java.util.Map.of("year", "2002"))
    )
    engine.compile("{{name}} ({{project.year}})").evaluate(values, writer)
    writer.toString shouldBe "Terminator (2002)"
  }

  private def evaluate(template: String, values: TemplateVariableValue*): String = {
    val writer = new StringWriter()
    engine.compile(template).evaluate(values, writer)
    writer.toString
  }
}
