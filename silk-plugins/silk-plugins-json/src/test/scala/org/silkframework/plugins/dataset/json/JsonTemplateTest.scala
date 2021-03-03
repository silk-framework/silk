package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.plugins.dataset.json.JsonTemplate.placeholder
import org.silkframework.runtime.validation.ValidationException

class JsonTemplateTest extends FlatSpec with Matchers {

  behavior of "JsonTemplate"

  it should "fail if the template is invalid" in {
    an[ValidationException] should be thrownBy { JsonTemplate.parse("") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse("[]") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse(s"[$placeholder,$placeholder]") }
  }

  it should "parse templates" in {
    JsonTemplate.parse(s"$placeholder") shouldBe JsonTemplate("", "")
    JsonTemplate.parse(s"prefix$placeholder") shouldBe JsonTemplate("prefix", "")
    JsonTemplate.parse(s"${placeholder}suffix") shouldBe JsonTemplate("", "suffix")
    JsonTemplate.parse(s"[$placeholder]") shouldBe JsonTemplate("[", "]")
  }

}
