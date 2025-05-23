package org.silkframework.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class IdentifierGeneratorTest extends AnyFlatSpec with Matchers {

  behavior of "IdentifierGenerator"

  it should "leave already unique identifiers unchanged" in {
    val generator = new IdentifierGenerator()
    generator.generate("person") mustBe Identifier("person")
    generator.generate("address") mustBe Identifier("address")
  }

  it should "append numbers in order to create unique identifiers" in {
    val generator = new IdentifierGenerator()
    generator.generate("address") mustBe Identifier("address")
    generator.generate("address") mustBe Identifier("address1")
    generator.generate("address") mustBe Identifier("address2")
  }

  it should "change postfix numbers to create unique identifiers" in {
    val generator = new IdentifierGenerator()
    generator.generate("address") mustBe Identifier("address")
    generator.generate("address") mustBe Identifier("address1")
    generator.generate("address1") mustBe Identifier("address2")
  }

  it should "not re-use existing identifiers" in {
    val generator = new IdentifierGenerator()
    generator.add("person")
    generator.add("person1")
    generator.generate("person") mustBe Identifier("person2")
    generator.generate("person1") mustBe Identifier("person3")
  }

  it should "not remove trailing numbers if not needed for generating a unique id" in {
    val generator = new IdentifierGenerator()
    generator.generate("name1") mustBe Identifier("name1")
  }
}
