package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.runtime.validation.ValidationException

class JsonTemplateTest extends FlatSpec with Matchers {

  behavior of "JsonTemplate"

  it should "fail if the template is invalid" in {
    an[ValidationException] should be thrownBy { JsonTemplate.parse("") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse("[]") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse("[{{entities}},{{entities}}]") }
  }

  it should "parse templates" in {
    JsonTemplate.parse("{{entities}}") shouldBe JsonTemplate("", "")
    JsonTemplate.parse("prefix{{entities}}") shouldBe JsonTemplate("prefix", "")
    JsonTemplate.parse("{{entities}}suffix") shouldBe JsonTemplate("", "suffix")
    JsonTemplate.parse("[{{entities}}]") shouldBe JsonTemplate("[", "]")
  }

}
