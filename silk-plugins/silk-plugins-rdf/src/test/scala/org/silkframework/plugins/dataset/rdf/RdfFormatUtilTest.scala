package org.silkframework.plugins.dataset.rdf

import org.scalatest.{MustMatchers, FlatSpec}

/**
  * Created on 9/8/16.
  */
class RdfFormatUtilTest extends FlatSpec with MustMatchers {
  behavior of "RDFFormatUtil"

  final val NL = "\n"

  it should "serialize object property triples" in {
    format("http://testurl") mustBe
      "<s> <p> <http://testurl> .\n"
  }

  private def format(objectValue: String): String = {
    RdfFormatUtil.tripleValuesToNTriplesSyntax("s", "p", objectValue)
  }

  it should "serialize double literal triples" in {
    format("3.14") mustBe
        s"""<s> <p> "3.14"^^<http://www.w3.org/2001/XMLSchema#double> .$NL"""
  }

  it should "serialize integer literal triples" in {
    format("33563267326578325683257832") mustBe
        s"""<s> <p> "33563267326578325683257832"^^<http://www.w3.org/2001/XMLSchema#integer> .$NL"""
  }

  it should "serialize all other to plain literal triples" in {
    format("some string") mustBe
        s"""<s> <p> "some string" .$NL"""
  }
}
