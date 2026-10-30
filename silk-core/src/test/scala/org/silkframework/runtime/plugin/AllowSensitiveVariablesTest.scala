package org.silkframework.runtime.plugin

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.plugin.annotations.Param
import org.silkframework.runtime.templating.{InMemoryTemplateVariablesReader, SimpleSubstitutionTemplateEngine, TemplateVariable, TemplateVariables, VariableScope}
import org.silkframework.util.ConfigTestTrait

class AllowSensitiveVariablesTest extends AnyFlatSpec with Matchers with ConfigTestTrait {

  behavior of "Plugin parameters that allow sensitive variables"

  override def propertyMap: Map[String, Option[String]] = Map("config.variables.engine" -> Some(SimpleSubstitutionTemplateEngine.id))

  private val secret = TemplateVariable("secret", "s3cret", isSensitive = true, scope = VariableScope.global)
  private implicit val pluginContext: PluginContext =
    TestPluginContext(templateVariables = InMemoryTemplateVariablesReader(TemplateVariables(Seq(secret)), Set(VariableScope.global)))
  private val pluginDesc = ClassPluginDescription(classOf[SensitiveVariablesTestPlugin])

  it should "resolve sensitive variables only for parameters that allow them and keep the value out of error messages" in {
    pluginDesc(templates("content" -> "key={{global.secret}}")).content shouldBe "key=s3cret"
    val withheld = intercept[InvalidPluginParameterValueException](pluginDesc(templates("other" -> "{{global.secret}}")))
    withheld.getMessage should include ("'global.secret' is not defined")
    withheld.getMessage should not include "s3cret"
    val invalid = intercept[InvalidPluginParameterValueException](pluginDesc(templates("number" -> "{{global.secret}}")))
    invalid.getMessage should include ("{{global.secret}}")
    invalid.getMessage should not include "s3cret"
  }

  private def templates(values: (String, String)*): ParameterValues = {
    ParameterValues(values.map { case (name, template) => (name, ParameterTemplateValue(template)) }.toMap)
  }
}

private case class SensitiveVariablesTestPlugin(@Param(value = "May hold a secret.", allowSensitiveVariables = true)
                                                content: String = "",
                                                @Param("Must not hold a secret.")
                                                other: String = "",
                                                @Param(value = "Typed parameter that may hold a secret.", allowSensitiveVariables = true)
                                                number: Int = 0) extends TestPluginType
