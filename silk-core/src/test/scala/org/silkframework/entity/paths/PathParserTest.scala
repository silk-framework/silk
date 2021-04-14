package org.silkframework.entity.paths

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes

class PathParserTest extends FlatSpec with MustMatchers {
  behavior of "path parser"

  val parser = new PathParser(Prefixes.default)
  private val SPACE = " "

  it should "parse the full path and not return any errors for valid paths" in {
    testValidPath("/a/b[d = 5]\\c")
    testValidPath("""\<urn:some:test>[<urn:prop:check> != "check value"]/abc""")
  }

  it should "parse invalid paths and return the parsed part and the position where it failed" in {
    testInvalidPath("/a/b/c/not valid/d/e", "/a/b/c/not", 10, SPACE)
    testInvalidPath(s"$SPACE/alreadyInvalid/a", "", 0, SPACE)
    testInvalidPath("""\<urn:test:1>/a[b :+ 1]/c""", "\\<urn:test:1>/a", 17, "[b ")
    testInvalidPath("""/a\b/c/""", """/a\b/c""",6, "/")
    testInvalidPath("""invalidNs:broken""", "",0, "")
    testInvalidPath("""<'""", "",0, "<")
  }

  private def testValidPath(inputString: String): Unit = {
    val result = parser.parseUntilError(inputString)
    result mustBe PartialParseResult(UntypedPath.parse(inputString), None)
  }

  private def testInvalidPath(inputString: String,
                              expectedParsedString: String,
                              expectedErrorOffset: Int,
                              expectedInputOnError: String): Unit = {
    val result = parser.parseUntilError(inputString)
    result.copy(error = result.error.map(e => e.copy(message = ""))) mustBe PartialParseResult(
      UntypedPath.parse(expectedParsedString),
      Some(PartialParseError(expectedErrorOffset, "", expectedInputOnError))
    )
  }
}
