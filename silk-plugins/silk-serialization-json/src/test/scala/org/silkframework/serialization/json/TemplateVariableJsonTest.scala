package org.silkframework.serialization.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.templating.{TemplateVariable, VariableScope}
import play.api.libs.json.Json

class TemplateVariableJsonTest extends AnyFlatSpec with Matchers {

  behavior of "TemplateVariableJson"

  it should "expose the value and template of non-sensitive variables when masking" in {
    val json = Json.toJson(TemplateVariableJson.masked(variable("endpoint", "https://example.org/sparql", "{{base}}/sparql", isSensitive = false)))
    (json \ "value").as[String] shouldBe "https://example.org/sparql"
    (json \ "template").as[String] shouldBe "{{base}}/sparql"
    (json \ "isSensitive").as[Boolean] shouldBe false
    (json \ "scope").as[String] shouldBe "project"
  }

  it should "mask the value and template of sensitive variables" in {
    val json = Json.toJson(TemplateVariableJson.masked(variable("password", "super-secret", "{{vault.secret}}", isSensitive = true)))
    (json \ "value").toOption shouldBe None
    (json \ "template").toOption shouldBe None
    (json \ "isSensitive").as[Boolean] shouldBe true
    // Metadata is still exposed
    (json \ "name").as[String] shouldBe "password"
    (json \ "description").as[String] shouldBe "The description"
    Json.stringify(json) should not include "super-secret"
    Json.stringify(json) should not include "vault.secret"
  }

  it should "not mask in the plain conversion" in {
    TemplateVariableJson(variable("password", "super-secret", "{{vault.secret}}", isSensitive = true)).value shouldBe Some("super-secret")
  }

  private def variable(name: String, value: String, template: String, isSensitive: Boolean): TemplateVariable = {
    TemplateVariable(name, value, Some(template), Some("The description"), isSensitive, VariableScope.project)
  }
}
