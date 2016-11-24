package org.silkframework.plugins.dataset.rdf

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity._

/**
  * Created on 9/8/16.
  */
class RdfFormatUtilTest extends FlatSpec with MustMatchers {
  behavior of "RDFFormatUtil"

  final val NL = "\n"
  final val S_P = "<s> <p>"

  it should "serialize object property triples" in {
    format("http://testurl") mustBe
      s"$S_P <http://testurl> .\n"
  }

  private def format(objectValue: String, valueType: ValueType = AutoDetectValueType): String = {
    RdfFormatUtil.tripleValuesToNTriplesSyntax("s", "p", objectValue, valueType)
  }

  it should "serialize double literal triples" in {
    format("3.14") mustBe
        s"""$S_P "3.14"^^<http://www.w3.org/2001/XMLSchema#double> .$NL"""
  }

  it should "serialize integer literal triples" in {
    format("33563267326578325683257832") mustBe
        s"""$S_P "33563267326578325683257832"^^<http://www.w3.org/2001/XMLSchema#integer> .$NL"""
  }

  it should "serialize all other to plain literal triples" in {
    format("some string") mustBe
        s"""$S_P "some string" .$NL"""
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
    format("bla45532532532532", BlankNodeValueType) must startWith
        s"""$S_P _:""" // Jena constructs new blank node ids
  }
}
