package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}

class DateTimeValueTypeTest extends FlatSpec with Matchers {

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

  it should "choose the most suitable XML Schema type" in {
    DateTimeValueType.xmlSchemaType("2017-08-23") shouldBe "http://www.w3.org/2001/XMLSchema#date"
    DateTimeValueType.xmlSchemaType("09:30:10") shouldBe "http://www.w3.org/2001/XMLSchema#time"
    DateTimeValueType.xmlSchemaType("2002-05-30T09:30:10") shouldBe "http://www.w3.org/2001/XMLSchema#dateTime"
  }

}
