package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity._

class RdfFormatUtilTest extends FlatSpec with MustMatchers {
  behavior of "RDFFormatUtil"

  final val NL = "\n"
  final val S_P = "<s> <p>"

  private def format(objectValue: String, valueType: ValueType = UntypedValueType): String = {
    RdfFormatUtil.tripleValuesToNTriplesSyntax("s", "p", objectValue, valueType)
  }

  private def expectedTriple(literalLexical: String): String = {
    s"""$S_P $literalLexical .$NL"""
  }

  it should "serialize double literal triples" in {
    format("3.14", valueType = DoubleValueType) mustBe
        s"""$S_P "3.14"^^<http://www.w3.org/2001/XMLSchema#double> .$NL"""
    format("12e3", valueType = DoubleValueType) mustBe expectedTriple("\"12e3\"^^<http://www.w3.org/2001/XMLSchema#double>")
    format("0.0e2", valueType = DoubleValueType) mustBe expectedTriple("\"0.0e2\"^^<http://www.w3.org/2001/XMLSchema#double>")
    // Whitespace allowed before and after
    format("42.1 ", valueType = DoubleValueType) mustBe expectedTriple("\"42.1 \"^^<http://www.w3.org/2001/XMLSchema#double>")
    format(" 42.1", valueType = DoubleValueType) mustBe expectedTriple("\" 42.1\"^^<http://www.w3.org/2001/XMLSchema#double>")
  }

  it should "serialize integer literal triples" in {
    format("33563267326578325683257832", valueType = IntValueType) mustBe
        s"""$S_P "33563267326578325683257832"^^<http://www.w3.org/2001/XMLSchema#integer> .$NL"""
    format("0", valueType = IntValueType) mustBe expectedTriple("\"0\"^^<http://www.w3.org/2001/XMLSchema#integer>")
  }

  it should "serialize all other to plain literal triples" in {
    format("some string") mustBe
        s"""$S_P "some string" .$NL"""
    format("41D") mustBe expectedTriple("\"41D\"")
    format("41d") mustBe expectedTriple("\"41d\"")
    format("41F") mustBe expectedTriple("\"41F\"")
    format("41f") mustBe expectedTriple("\"41f\"")
    format("000") mustBe expectedTriple("\"000\"")
    format("2ea") mustBe expectedTriple("\"2ea\"")
  }

  it should "serialize triples with StringValueType" in {
    format("31", StringValueType) mustBe
        s"""$S_P "31" .$NL"""
  }

  it should "serialize triples with LanguageValueType" in {
    format("text", LanguageValueType("en")) mustBe
        s"""$S_P "text"@en .$NL"""
  }

  it should "serialize triples with UriValueType" in {
    format("urn:test:31", UriValueType) mustBe
        s"""$S_P <urn:test:31> .$NL"""
  }

  it should "serialize triples with CustomValueType" in {
    format("custom", CustomValueType("http://custom")) mustBe
        s"""$S_P "custom"^^<http://custom> .$NL"""
  }

  it should "serialize triples with BlankNodeValueType" in {
    format("bla45532532532532", BlankNodeValueType) must startWith regex
        s"""$S_P _:""" // Jena constructs new blank node ids
  }

  it should "serialize triples with BooleanValueType" in {
    format("true", BooleanValueType) mustBe
        s"""$S_P "true"^^<http://www.w3.org/2001/XMLSchema#boolean> .$NL"""
  }

  it should "serialize triples with DoubleValueType" in {
    format("4.2", DoubleValueType) mustBe
        s"""$S_P "4.2"^^<http://www.w3.org/2001/XMLSchema#double> .$NL"""
  }

  it should "serialize triples with FloatValueType" in {
    format("2.3", FloatValueType) mustBe
        s"""$S_P "2.3"^^<http://www.w3.org/2001/XMLSchema#float> .$NL"""
  }

  it should "serialize triples with IntValueType" in {
    format("42", IntValueType) mustBe
        s"""$S_P "42"^^<http://www.w3.org/2001/XMLSchema#integer> .$NL"""
  }

  it should "serialize triples with IntegerValueType" in {
    format("543255432543254322354444444432", IntegerValueType) mustBe
        s"""$S_P "543255432543254322354444444432"^^<http://www.w3.org/2001/XMLSchema#integer> .$NL"""
  }

  it should "serialize triples with LongValueType" in {
    format("5432554325432542235", LongValueType) mustBe
        s"""$S_P "5432554325432542235"^^<http://www.w3.org/2001/XMLSchema#long> .$NL"""
  }

  it should "serialize triples with DateValueType" in {
    format("2015-04-03", DateValueType) mustBe
      s"""$S_P "2015-04-03"^^<http://www.w3.org/2001/XMLSchema#date> .$NL"""
  }

  it should "serialize triples with DateTimeValueType" in {
    format("2002-05-30T09:00:00", DateTimeValueType) mustBe
      s"""$S_P "2002-05-30T09:00:00"^^<http://www.w3.org/2001/XMLSchema#dateTime> .$NL"""
  }

  it should "serialize triples with UntypedValueType always as plain literal" in {
    format("1024", UntypedValueType) mustBe
      s"""$S_P "1024" .$NL"""
    format("http://example.org/resource", UntypedValueType) mustBe
        s"""$S_P "http://example.org/resource" .$NL"""
  }
}
