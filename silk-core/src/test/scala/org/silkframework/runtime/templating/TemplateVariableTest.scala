package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.templating.exceptions.{SensitiveVariableReferenceException, TemplateVariablesEvaluationException, UnboundVariablesException}
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.ConfigTestTrait

class TemplateVariableTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  override def propertyMap: Map[String, Option[String]] = Map("config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id))

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

  behavior of "TemplateVariables"

  it should "not resolve a non-sensitive variable against a sensitive variable of the same scope" in {
    val variables = TemplateVariables(Seq(
      TemplateVariable("password", "secret", None, None, isSensitive = true, VariableScope.project),
      TemplateVariable("dbUrl", "", Some("jdbc://{{project.password}}@host"), None, isSensitive = false, VariableScope.project)))
    // The referenced variable is defined, so the error names the sensitivity rule instead of an undefined variable
    val ex = the[TemplateVariablesEvaluationException] thrownBy variables.resolved()
    ex.issues.map(_.ex.getClass) shouldBe Seq(classOf[SensitiveVariableReferenceException])
    ex.getMessage shouldBe "Variable 'dbUrl': 'project.password' is sensitive and can only be referenced from a sensitive variable."
    // The lenient variant keeps the stored value
    variables.resolvedKeepingUnresolved().map("dbUrl").value shouldBe ""
    // An undefined variable in the same template is reported along, so that both problems show up at once
    val withUndefined = TemplateVariables(Seq(
      TemplateVariable("password", "secret", None, None, isSensitive = true, VariableScope.project),
      TemplateVariable("dbUrl", "", Some("jdbc://{{project.password}}@{{project.host}}"), None, isSensitive = false, VariableScope.project)))
    the[TemplateVariablesEvaluationException] thrownBy withUndefined.resolved() should have message
      "Variable 'dbUrl': 'project.password' is sensitive and can only be referenced from a sensitive variable. 'project.host' is not defined."
  }

  it should "report a reference to a variable that is not defined as undefined" in {
    val variables = TemplateVariables(Seq(
      TemplateVariable("dbUrl", "", Some("jdbc://{{project.password}}@host"), None, isSensitive = false, VariableScope.project)))
    val ex = the[TemplateVariablesEvaluationException] thrownBy variables.resolved()
    ex.issues.map(_.ex.getClass) shouldBe Seq(classOf[UnboundVariablesException])
  }

  it should "resolve a sensitive variable against a sensitive variable of the same scope" in {
    val variables = TemplateVariables(Seq(
      TemplateVariable("password", "secret", None, None, isSensitive = true, VariableScope.project),
      TemplateVariable("dbUrl", "", Some("jdbc://{{project.password}}@host"), None, isSensitive = true, VariableScope.project)))
    variables.resolved().map("dbUrl").value shouldBe "jdbc://secret@host"
  }

  private def variableName(name: String) = TemplateVariable(name, "test value", None, None, isSensitive = false, VariableScope("testScope"))

}
