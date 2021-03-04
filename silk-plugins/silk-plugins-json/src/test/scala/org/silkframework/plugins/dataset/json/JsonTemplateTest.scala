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
    an[ValidationException] should be thrownBy { JsonTemplate.parse("no valid JSON") }
  }

  it should "parse templates" in {
    JsonTemplate.parse(s"$placeholder") shouldBe JsonTemplate("", "")
    JsonTemplate.parse(s"[$placeholder]") shouldBe JsonTemplate("[", "]")
    JsonTemplate.parse(s"""{ "metaData": { "timestamp": "2021-01-01" }, "data": $placeholder }""") shouldBe
      JsonTemplate("""{ "metaData": { "timestamp": "2021-01-01" }, "data": """, " }")
  }

}
