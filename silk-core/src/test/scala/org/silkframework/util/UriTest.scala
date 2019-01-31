package org.silkframework.util

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes

class UriTest extends FlatSpec with Matchers {

  val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#"
  )

  behavior of "URI"

  it should "parse full URIs" in {
    Uri.parse("<http://example.org/entity1>").uri shouldBe "http://example.org/entity1"
    Uri.parse("http://example.org/entity1").uri shouldBe "http://example.org/entity1"
  }

  it should "parse prefix names" in {
    Uri.parse("rdfs:label", prefixes).uri shouldBe "http://www.w3.org/2000/01/rdf-schema#label"
  }

  it should "detect valid URIs" in {
    Uri("http://example.org/entity1").isValidUri shouldBe true
    Uri("http://example.org/###").isValidUri shouldBe false
    Uri("example.org/entity1").isValidUri shouldBe false
    Uri("").isValidUri shouldBe false
  }

  it should "extract the local name" in {
    Uri("http://example.org/parent/name").localName shouldBe Some("name")
    Uri("http://example.org/name#fragment").localName shouldBe Some("fragment")
    Uri("urn:namespace:name").localName shouldBe Some("name")
    Uri("urn:namespace:name/child").localName shouldBe Some("child")
    Uri("http://example.org").localName shouldBe None
  }

}
