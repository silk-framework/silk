package org.silkframework.entity.paths

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes

class PathParserTest extends FlatSpec with MustMatchers {
  behavior of "path parser"

  val parser = new PathParser(Prefixes.default)

  it should "parse the full path and not return any errors for valid paths" in {
    testValidPath("/a/b[d = 5]\\c")
    testValidPath("""\<urn:some:test>[<urn:prop:check> != "check value"]/abc""")
  }

  it should "parse invalid paths and return the parsed part and the position where it failed" in {
    testInvalidPath("/a/b/c/not valid/d/e", "/a/b/c/not", 10)
    testInvalidPath(" /alreadyInvalid/a", "", 0)
    testInvalidPath("""\<urn:test:1>/a[b :+ 1]/c""", "\\<urn:test:1>/a", 17)
    testInvalidPath("""/a\b/c/""", """/a\b/c""",6)
  }

  private def testValidPath(inputString: String): Unit = {
    val result = parser.parseUntilError(inputString)
    result mustBe PartialParseResult(UntypedPath.parse(inputString), None)
  }

  private def testInvalidPath(inputString: String, expectedParsedString: String, expectedErrorOffset: Int): Unit = {
    val result = parser.parseUntilError(inputString)
    result.copy(error = result.error.map(e => e.copy(message = ""))) mustBe PartialParseResult(
      UntypedPath.parse(expectedParsedString),
      Some(PartialParseError(expectedErrorOffset, ""))
    )
  }
}
