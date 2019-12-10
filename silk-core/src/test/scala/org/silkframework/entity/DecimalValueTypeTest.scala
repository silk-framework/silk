package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}

class DecimalValueTypeTest extends FlatSpec with Matchers {

  behavior of "DecimalValueType"

  it should "accept valid XML schema decimals" in {
    DecimalValueType.validate("+1234.456") shouldBe true
    DecimalValueType.validate("1234567890123456789012345678901234567890.1234567890") shouldBe true
  }

  it should "reject invalid XML schema decimals" in {
    DecimalValueType.validate("1.7.2017") shouldBe false
  }

}
