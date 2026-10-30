package controllers.workspaceApi

import controllers.workspaceApi.coreApi.variableTemplate.{ValidateVariableTemplateRequest, VariableTemplateValidationResponse}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.plugins.templating.jinja.JinjaTemplateEngine
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{ClassPluginDescription, InvalidPluginParameterValueException, ParameterStringValue, ParameterTemplateValue, ParameterValues, TestPluginContext}
import org.silkframework.runtime.templating.TemplateVariables
import org.silkframework.util.ConfigTestTrait

/**
  * Tests template validation with the Jinja engine.
  * Complements [[VariableTemplateApiTest]], which runs on the simple substitution engine.
  */
class VariableTemplateJinjaValidationTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "variable template validation with the Jinja engine"

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(JinjaTemplateEngine.id),
    "config.variables.global.myVar" -> Some("globalValue"),
    "config.variables.global.secret.value" -> Some("s3cret"),
    "config.variables.global.secret.isSensitive" -> Some("true")
  )

  private implicit val user: UserContext = UserContext.Empty

  it should "not report unbound variables if they are ignored" in {
    // Regression (CMEM-7147): iterating over a variable that is only bound at execution time
    // (e.g. 'entities' of the template operator) was reported as a missing variable.
    validate("{% for entity in entities %}{{ entity.name }}{% endfor %}", lenient = true).valid shouldBe true
  }

  it should "not report value-dependent evaluation errors if unbound variables are ignored" in {
    validate("{{ price * 2 }}", lenient = true).valid shouldBe true
  }

  it should "report syntax errors even if unbound variables are ignored" in {
    val response = validate("{{ name | }}", lenient = true)
    response.valid shouldBe false
    response.parseError.map(_.message).getOrElse("") should include ("syntax error")
  }

  it should "report unbound variables if they are not ignored" in {
    validate("{{ unknownVar }}", lenient = false).valid shouldBe false
  }

  it should "report missing global variables even if unbound variables are ignored" in {
    // The global scope is always fully known, so its references are checked in lenient mode too
    validate("{{ global.unknownVar }}", lenient = true).valid shouldBe false
    // Without a project or task context, other scopes stay tolerated
    validate("{{ project.unknownVar }}", lenient = true).valid shouldBe true
    validate("{{ execution.unknownVar }}", lenient = true).valid shouldBe true
  }

  it should "underline only the missing variable, not the whole template" in {
    val template = "{{ global.unknownVar }}\n\ntext\n\n{{ global.myVar }}"
    val response = validate(template, lenient = true)
    response.valid shouldBe false
    val error = response.parseError.get
    error.start shouldBe template.indexOf("global.unknownVar")
    error.end shouldBe template.indexOf("global.unknownVar") + "global.unknownVar".length
  }

  it should "distinguish method calls from property access on existing variables" in {
    // A method call references the variable itself and resolves at execution time
    validate("{{ global.myVar.trim() ~ 'x' }}", lenient = true).valid shouldBe true
    // Property access does not resolve on variable values, so it is reported
    validate("{{ global.myVar.length }}", lenient = true).valid shouldBe false
  }

  it should "keep sensitive values out of the preview and the errors of parameter templates that compute with them" in {
    val template = "{{ 5 | add(global.secret) }}"
    val preview = ValidateVariableTemplateRequest(template, includeSensitiveVariables = Some(true)).execute()
    preview.valid shouldBe true
    preview.evaluatedTemplate shouldBe Some(TemplateVariables.SensitiveValuePlaceholder)
    val error = intercept[InvalidPluginParameterValueException] {
      ClassPluginDescription(classOf[VariablesTestTask])(ParameterValues(Map(
        "title" -> ParameterStringValue("T"), "year" -> ParameterStringValue("2002"), "secretContent" -> ParameterTemplateValue(template))))(TestPluginContext())
    }
    error.getMessage should include (template)
    error.getMessage should not include "s3cret"
  }

  private def validate(template: String, lenient: Boolean): VariableTemplateValidationResponse = {
    ValidateVariableTemplateRequest(template, ignoreUnboundVariables = Some(lenient)).execute()
  }
}
