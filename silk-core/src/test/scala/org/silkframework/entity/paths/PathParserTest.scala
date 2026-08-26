package org.silkframework.entity.paths

import org.silkframework.config.Prefixes
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PathParserTest extends AnyFlatSpec with Matchers {
  behavior of "path parser"

  val parser = new PathParser(Prefixes.default)
  private val SPACE = " "

  it should "parse the full path and not return any errors for valid paths" in {
    testValidPath("/a/b[d = 5]\\c")
    testValidPath("""\<urn:some:test>[<urn:prop:check> != "check value"]/abc""")
    testValidPath("""abc[@lang ='en']""").operators.drop(1).head mustBe LanguageFilter("=", "en")
    testValidPath("""abc[ @lang   =  'en']""").operators.drop(1).head mustBe LanguageFilter("=", "en")
  }

  it should "parse property filters with all comparison operators, including two-character ones" in {
    for(op <- Seq(">=", "<=", "!=", ">", "<", "=")) {
      val path = testValidPath(s"""/person[age $op "18"]""")
      path.operators.drop(1).head mustBe PropertyFilter(Uri("age"), op, "\"18\"")
    }
  }

  it should "parse invalid paths and return the parsed part and the position where it failed" in {
    testInvalidPath(inputString = "/a/b/c/not valid/d/e", expectedParsedString = "/a/b/c/not", expectedErrorOffset = 10, expectedInputOnError = SPACE, expectedNextParseOffset = "/a/b/c/not".length)
    testInvalidPath(inputString = s"$SPACE/alreadyInvalid/a", expectedParsedString = "", expectedErrorOffset = 0, expectedInputOnError = SPACE, expectedNextParseOffset = 0)
    testInvalidPath(inputString = """\<urn:test:1>/a[b :+ 1]/c""", expectedParsedString = "\\<urn:test:1>/a", expectedErrorOffset = 17, expectedInputOnError = "[b ", expectedNextParseOffset = "\\<urn:test:1>/a".length)
    testInvalidPath(inputString = """/a\b/c/""", expectedParsedString = """/a\b/c""",expectedErrorOffset = 6, expectedInputOnError = "/", expectedNextParseOffset = 6)
    testInvalidPath(inputString = """invalidNs:broken""", expectedParsedString = "",expectedErrorOffset = 0, expectedInputOnError = "", expectedNextParseOffset = 0)
    testInvalidPath(inputString = """<'""", expectedParsedString = "",expectedErrorOffset = 0, expectedInputOnError = "<", expectedNextParseOffset = 0)
  }

  it should "parse a path that starts with a filter, which is how such a path is serialized" in {
    val path = testValidPath("""[a = "b"]/c""")
    path.operators.head mustBe PropertyFilter(Uri("a"), "=", "\"b\"")
  }

  it should "parse back a serialized path whose first operator is a filter" in {
    val path = UntypedPath(List(PropertyFilter(Uri("a"), "=", "\"b\""), ForwardOperator(Uri("c"))))
    UntypedPath.parse(path.normalizedSerialization) mustBe path
  }

  // The caller never wrote the variable the parser adds, and cannot act on the grammar.

  it should "report a parse error at its position in the path the caller wrote" in {
    val error = intercept[ValidationException](parser.parse("A/[B"))
    error.getMessage must startWith("Cannot parse the path at character 3: a property name or a <URI> expected but '[' found")
    error.getMessage must include("A/[B\n  ^")
    error.getMessage must not include "?a"
  }

  it should "name what it expected instead of printing the grammar" in {
    parser.parseUntilError("a/[").error.get.message must include("a property name or a <URI>")
    intercept[ValidationException](parser.parse("a/[")).getMessage must not include "string matching regex"
  }

  it should "partially parse filter expressions correctly" in {
    parser.parseUntilError("""a/b[@lang  =  "en"]""").error must not be defined
    val result = parser.parseUntilError("""a/b[@lang  =  "en"]/error now""")
    val error = result.error
    error mustBe defined
    error.get.offset mustBe """a/b[@lang  =  "en"]/error""".length
  }

  private def testValidPath(inputString: String): UntypedPath = {
    val result = parser.parseUntilError(inputString)
    result mustBe PartialParseResult(UntypedPath.parse(inputString), None)
    result.partialPath
  }

  private def testInvalidPath(inputString: String,
                              expectedParsedString: String,
                              expectedErrorOffset: Int,
                              expectedInputOnError: String,
                              expectedNextParseOffset: Int): Unit = {
    val result = parser.parseUntilError(inputString)
    result.copy(error = result.error.map(e => e.copy(message = ""))) mustBe PartialParseResult(
      UntypedPath.parse(expectedParsedString),
      Some(PartialParseError(expectedErrorOffset, "", expectedInputOnError, expectedNextParseOffset))
    )
  }
}
