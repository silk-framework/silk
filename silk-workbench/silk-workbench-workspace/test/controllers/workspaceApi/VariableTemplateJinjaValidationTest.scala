package controllers.workspaceApi

import controllers.workspaceApi.coreApi.variableTemplate.{ValidateVariableTemplateRequest, VariableTemplateValidationResponse}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.plugins.templating.jinja.JinjaTemplateEngine
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.ConfigTestTrait

/**
  * Tests template validation with the Jinja engine.
  * Complements [[VariableTemplateApiTest]], which runs on the simple substitution engine.
  */
class VariableTemplateJinjaValidationTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "variable template validation with the Jinja engine"

  override def propertyMap: Map[String, Option[String]] = Map(
    "config.variables.engine" -> Some(JinjaTemplateEngine.id)
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

  private def validate(template: String, lenient: Boolean): VariableTemplateValidationResponse = {
    ValidateVariableTemplateRequest(template, ignoreUnboundVariables = Some(lenient)).execute()
  }
}
