package org.silkframework.rule.util


import org.silkframework.config.Prefixes
import org.silkframework.rule.util.UriPatternParser.UriPatternParserException

import scala.util.{Failure, Success, Try}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class UriPatternParserTest extends AnyFlatSpec with Matchers {
  behavior of "URI pattern parser"

  it should "fail immediately for syntactically wrong templates" in {
    val failPatterns = Seq(
      "{{}",
      "}",
      "{noClosing",
      "constant{<http{>}",
      "constant{pat{h}",
      "constant{path{"
    )
    for(failPattern <- failPatterns) {
      Try(UriPatternParser.parseIntoSegments(failPattern, allowIncompletePattern = false)) match {
        case Success(_) =>
          throw new AssertionError(s"URI Pattern '$failPattern' should have failed, but did not.")
        case Failure(_: UriPatternParserException) =>
        case Failure(exception) =>
          throw new AssertionError(s"For URI pattern '$failPattern' expected a UriPatternParserException to be thrown, but instead a ${exception.getClass.getSimpleName} has been thrown.")
      }
    }
  }

  it should "segment syntactically correct URI patterns" in {
    val validPatterns = Seq(
      "",
      "{}",
      "constant{}",
      "constant{path}constant",
      """urn:prop:{path[pathB = "Here these are valid: {{}"]}"""
    )
    for(validPattern <- validPatterns) {
      Try(UriPatternParser.parseIntoSegments(validPattern, allowIncompletePattern = false)) match {
        case Success(_) =>
        case Failure(ex: UriPatternParserException) =>
          throw new AssertionError(s"Valid URI pattern '$validPattern' was not segmented successfully. Error details: $ex.")
        case Failure(ex) =>
          throw ex
      }
    }
  }

  def validate(pattern: String): UriPatternValidationResult = {
    implicit val prefixes: Prefixes = Prefixes.default
    val segments = UriPatternParser.parseIntoSegments(pattern, allowIncompletePattern = false)
    segments.validationResult()
  }
  def validateSuccessful(pattern: String): Unit = {
    val result = validate(pattern)
    if(!result.success) {
      throw new AssertionError(s"URI pattern '$pattern' should have been validated as correct, but was not. Cause: ${result.validationError.get.msg}")
    }
  }

  def validateAsIncorrect(pattern: String): Unit = {
    val result = validate(pattern)
    if(result.success) {
      throw new AssertionError(s"URI pattern '$pattern' should have been validated as incorrect, but it was correct.")
    }
  }

  it should "successfully validate correct URI patterns" in {
    validateSuccessful("{}")
    validateSuccessful("urn:{somepath}")
    validateSuccessful("""http://{<http://test.test/path?query=value>/ABC[path = "match this"]}""")
    validateSuccessful("{}{somepath}")
    validateSuccessful("ftp://{}")
    validateSuccessful("urn:{rdf:label}")
    validateSuccessful("""http://{<http://test.test/path?query=value>/ABC[path = "{}"]}""")
    validateSuccessful("{}path/{pathA}/pathB/{pathC}")
  }

  it should "validate incorrect URI patterns as so" in {
    validateAsIncorrect("")
    validateAsIncorrect("notUri/{}")
    validateAsIncorrect("urn:in valid")
    validateAsIncorrect("{}/not\"valid")
    validateAsIncorrect("urn:{not valid}")
    validateAsIncorrect("""urn:{path[not valid = ""]}""")
    validateAsIncorrect("{}not<valid{path}")
    validateAsIncorrect("urn:{unknown:path}")
    validate("invalid").validationError.map(_.errorRange) mustBe Some((0, "invalid".length))
  }
}
