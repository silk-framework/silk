package org.silkframework.util


import org.silkframework.config.Prefixes
import org.silkframework.runtime.validation.ValidationException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UriTest extends AnyFlatSpec with Matchers {

  val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#"
  )

  behavior of "URI"

  it should "parse full URIs" in {
    Uri.parse("<http://example.org/entity1>").uri shouldBe "http://example.org/entity1"
    Uri.parse("http://example.org/entity1").uri shouldBe "http://example.org/entity1"
  }

  it should "reject URIs with an unterminated angle bracket" in {
    // Used to silently return http://example.org/entity (last character cut off)
    an[ValidationException] should be thrownBy Uri.parse("<http://example.org/entity1")
    an[ValidationException] should be thrownBy Uri.parse("<")
    an[ValidationException] should be thrownBy Uri.parse("<>x")
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

  it should "url decode the local name" in {
    Uri.urlDecodedLocalNameOfURI("http://example.org/caf%C3%A9") shouldBe "café"
    Uri.urlDecodedLocalNameOfURI("http://example.org/with%20space") shouldBe "with space"
    // '+' is a literal in a URI, it only means space in form encoding
    Uri.urlDecodedLocalNameOfURI("http://example.org/a+b") shouldBe "a+b"
    // A stray '%' is not a valid escape sequence, but must not fail the label extraction
    Uri.urlDecodedLocalNameOfURI("http://example.org/50%") shouldBe "50%"
    Uri.urlDecodedLocalNameOfURI("http://example.org/100%25") shouldBe "100%"
  }

  it should "extract the namespace" in {
    Uri("http://example.org/resource/path?x=y").namespace shouldBe Some("http://example.org/resource/")
    Uri("urn:example:resource").namespace shouldBe Some("urn:example:")
    Uri("resource").namespace shouldBe None
    Uri("http://example.org/resource#fragment").namespace shouldBe Some("http://example.org/resource#")
  }

}
