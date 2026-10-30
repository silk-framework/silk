package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.types.PasswordParameter
import org.silkframework.util.ConfigTestTrait

class GlobalTemplateVariablesTest extends AnyFlatSpec with Matchers {

  behavior of "GlobalTemplateVariables"

  it should "load global variables" in {
    ConfigTestTrait.updateAndBackupParameters(Map(
      "config.variables.global.simpleVar" -> Some("value 1"),
      "config.variables.global.complexVar.value" -> Some("value 2"),
      "config.variables.global.complexVar.description" -> Some("my description"),
      "config.variables.global.complexVar.isSensitive" -> Some("false"),
      "config.variables.global.sensitiveVar.value" -> Some("value 3"),
      "config.variables.global.sensitiveVar.isSensitive" -> Some("true")
    ))

    val variables = GlobalTemplateVariables.all

    variables.map.get("simpleVar") shouldBe
      Some(TemplateVariable(
        name = "simpleVar",
        value = "value 1",
        template = None,
        description = None,
        isSensitive = false,
        scope = VariableScope.global
      ))

    variables.map.get("complexVar") shouldBe
      Some(TemplateVariable(
        name = "complexVar",
        value = "value 2",
        template = None,
        description = Some("my description"),
        isSensitive = false,
        scope = VariableScope.global
      ))

    variables.map.get("sensitiveVar") shouldBe
      Some(TemplateVariable(
        name = "sensitiveVar",
        value = "value 3",
        template = None,
        description = None,
        isSensitive = true,
        scope = VariableScope.global
      ))
  }

  it should "decrypt encrypted values of sensitive variables, keep them literally in other variables and leave out undecryptable ones" in {
    val encrypted = PasswordParameter.encrypt("value 4").toString
    ConfigTestTrait.withConfig(
      "config.variables.global.encryptedVar.value" -> Some(encrypted),
      "config.variables.global.encryptedVar.isSensitive" -> Some("true"),
      "config.variables.global.plainVar.value" -> Some(encrypted),
      "config.variables.global.plainVar.isSensitive" -> Some("false"),
      "config.variables.global.brokenVar.value" -> Some(encrypted + "x"),
      "config.variables.global.brokenVar.isSensitive" -> Some("true")
    ) {
      val variables = GlobalTemplateVariables.all

      variables.map("encryptedVar").value shouldBe "value 4"
      variables.map("plainVar").value shouldBe encrypted
      variables.map.get("brokenVar") shouldBe None
    }
  }

}
