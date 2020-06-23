package org.silkframework.entity

import org.scalatest.{FlatSpec, Matchers}

class DateTimeValueTypeTest extends FlatSpec with Matchers {

  behavior of "DateTimeValueType"

  private val dt = AnyDateTimeValueType()

  it should "accept XML schema dates" in {
    dt.validate("2017-08-23") shouldBe true
    dt.validate("2002-09-24-06:00") shouldBe true
    dt.validate("09:30:10") shouldBe true
    dt.validate("09:30:10.5") shouldBe true
    dt.validate("2002-05-30T09:30:10") shouldBe true
  }

  it should "reject invalid XML schema dates" in {
    dt.validate("2017-08-XX") shouldBe false
    dt.validate("1.7.2017") shouldBe false
    dt.validate("9:30") shouldBe false
  }

  it should "choose the most suitable XML Schema type" in {
    dt.xmlSchemaType("2017-08-23") shouldBe "http://www.w3.org/2001/XMLSchema#date"
    dt.xmlSchemaType("09:30:10") shouldBe "http://www.w3.org/2001/XMLSchema#time"
    dt.xmlSchemaType("2002-05-30T09:30:10") shouldBe "http://www.w3.org/2001/XMLSchema#dateTime"
  }

}
