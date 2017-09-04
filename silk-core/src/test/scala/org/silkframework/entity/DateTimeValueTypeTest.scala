package org.silkframework.entity

import org.scalatest.{FlatSpec, ShouldMatchers}

class DateTimeValueTypeTest extends FlatSpec with ShouldMatchers {

  behavior of "DateTimeValue"

  it should "accept XML schema dates" in {
    DateTimeValueType.validate("2017-08-23") shouldBe true
    DateTimeValueType.validate("2002-09-24-06:00") shouldBe true
    DateTimeValueType.validate("09:30:10") shouldBe true
    DateTimeValueType.validate("09:30:10.5") shouldBe true
    DateTimeValueType.validate("2002-05-30T09:30:10") shouldBe true
  }

  it should "reject invalid XML schema dates" in {
    DateTimeValueType.validate("2017-08-XX") shouldBe false
    DateTimeValueType.validate("1.7.2017") shouldBe false
    DateTimeValueType.validate("9:30") shouldBe false
  }

}
