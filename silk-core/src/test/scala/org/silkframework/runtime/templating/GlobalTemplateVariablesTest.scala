package org.silkframework.runtime.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
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
        scope = TemplateVariableScopes.global
      ))

    variables.map.get("complexVar") shouldBe
      Some(TemplateVariable(
        name = "complexVar",
        value = "value 2",
        template = None,
        description = Some("my description"),
        isSensitive = false,
        scope = TemplateVariableScopes.global
      ))

    variables.map.get("sensitiveVar") shouldBe
      Some(TemplateVariable(
        name = "sensitiveVar",
        value = "value 3",
        template = None,
        description = None,
        isSensitive = true,
        scope = TemplateVariableScopes.global
      ))
  }

}
