package org.silkframework.rule

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.validation.ValidationException

class MappingRulesTest extends AnyFlatSpec with Matchers {

  behavior of "MappingRules.fromSeq"

  it should "reject a second URI rule instead of discarding it" in {
    val ex = intercept[ValidationException] {
      MappingRules.fromSeq(Seq(PatternUriMapping(id = "first"), PatternUriMapping(id = "second")))
    }
    ex.getMessage should include ("first")
    ex.getMessage should include ("second")
  }

  it should "keep a single URI rule and separate type from property rules" in {
    val rules = MappingRules.fromSeq(Seq(PatternUriMapping(id = "uri"), TypeMapping(id = "type"), DirectMapping(id = "name")))
    rules.uriRule.map(_.id.toString) shouldBe Some("uri")
    rules.typeRules.map(_.id.toString) shouldBe Seq("type")
    rules.propertyRules.map(_.id.toString) shouldBe Seq("name")
  }
}
