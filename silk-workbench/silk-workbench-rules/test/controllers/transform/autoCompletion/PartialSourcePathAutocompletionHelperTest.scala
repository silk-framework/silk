package controllers.transform.autoCompletion

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes

class PartialSourcePathAutocompletionHelperTest extends FlatSpec with MustMatchers {
  behavior of "PartialSourcePathAutocompletionHelper"

  def replace(inputString: String,
              cursorPosition: Int,
              subPathOnly: Boolean = false): PathToReplace = {
    PartialSourcePathAutocompletionHelper.pathToReplace(PartialSourcePathAutoCompletionRequest(inputString, cursorPosition, None), subPathOnly)(Prefixes.empty)
  }

  it should "correctly find out which part of a path to replace in simple forward paths for the sub path the cursor is in" in {
    val input = "a1/b1/c1"
    replace(input, 4, subPathOnly = true) mustBe PathToReplace(2, 3, Some("b1"))
    replace(input, 5, subPathOnly = true) mustBe PathToReplace(2, 3, Some("b1"))
  }

  it should "correctly find out which part of a path to replace in simple forward paths for the path prefix the cursor is in " in {
    val input = "a1/b1/c1"
    replace(input, 4) mustBe PathToReplace(2, 6, Some("b1"))
    replace(input, 5) mustBe PathToReplace(2, 6, Some("b1"))
  }

  it should "correctly find out what part to replace in mixed forward and backward paths" in {
    val input = """\<urn:prop:p1>/qns:propper/<p:lastPath>"""
    val initialCursorPosition = "\\<urn:prop:p1>/".length
    for(cursorPosition <- initialCursorPosition to (initialCursorPosition + 11)) {
      replace(input, cursorPosition, subPathOnly = true) mustBe PathToReplace(initialCursorPosition - 1, "/qns:propper".length, Some("propper"))
    }
  }

  it should "correctly find out what to replace in filter expressions" in {
    val inputString = """a/b[@lang  =  "en"]/error now"""
    val inputString2 = """a/b[c = "val"]/d"""
    // Check that expressions with filter containing a lot of whitespace lead to correct results
    replace(inputString, inputString.length - 5) mustBe
      PathToReplace("a/b[@lang  =  \"en\"]".length, "/error now".length, Some("error now"))
    replace(inputString2, "a/b[c".length) mustBe
      PathToReplace("a/b[".length, 1, Some("c"), insideFilter = true)
  }

  it should "know if the cursor is inside a filter expression" in {
    val path = """department[id = "department X"]/tags[id"""
    replace(path, path.length).insideFilter mustBe true
  }
}
