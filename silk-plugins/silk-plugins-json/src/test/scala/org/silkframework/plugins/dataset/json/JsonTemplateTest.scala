package org.silkframework.plugins.dataset.json

import org.silkframework.plugins.dataset.json.JsonTemplate.placeholder
import org.silkframework.runtime.validation.ValidationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonTemplateTest extends AnyFlatSpec with Matchers {

  behavior of "JsonTemplate"

  it should "fail if the template is invalid" in {
    an[ValidationException] should be thrownBy { JsonTemplate.parse("") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse("[]") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse(s"[$placeholder,$placeholder]") }
    an[ValidationException] should be thrownBy { JsonTemplate.parse(s"no valid JSON $placeholder") }
  }

  it should "parse templates" in {
    JsonTemplate.parse(s"$placeholder") shouldBe JsonTemplate("", "")
    JsonTemplate.parse(s"[$placeholder]") shouldBe JsonTemplate("[", "]")
    JsonTemplate.parse(s"""{ "metaData": { "timestamp": "2021-01-01" }, "data": $placeholder }""") shouldBe
      JsonTemplate("""{ "metaData": { "timestamp": "2021-01-01" }, "data": """, " }")
  }

}
