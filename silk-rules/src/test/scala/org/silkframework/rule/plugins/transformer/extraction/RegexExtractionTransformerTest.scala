package org.silkframework.rule.plugins.transformer.extraction

import org.scalatest.{FlatSpec, MustMatchers}

/**
  *
  */
class RegexExtractionTransformerTest extends FlatSpec with MustMatchers {
  behavior of "Regex Extraction Transformer"

  it should "return the extracted result of a full match" in {
    val regex = """[a-z]{2,4}123"""
    val transformer = RegexExtractionTransformer(regex)
    transformer.apply(Seq(Seq("afe123"))) mustBe List("afe123")
  }

  it should "return an empty list if nothing matches" in {
    val regex = """^[a-z]{2,4}123"""
    val transformer = RegexExtractionTransformer(regex)
    transformer.apply(Seq(Seq("abcdef123"))) mustBe List()
  }

  it should "return the match of the first capture group that matches" in {
    val regex = """^([a-z]{2,4})123([a-z]+)"""
    val transformer = RegexExtractionTransformer(regex)
    transformer.apply(Seq(Seq("abcd123xyz"))) mustBe List("abcd")
  }
}
