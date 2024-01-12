package controllers.transform.autoCompletion


import org.silkframework.config.Prefixes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PartialSourcePathAutocompletionHelperTest extends AnyFlatSpec with Matchers {
  behavior of "PartialSourcePathAutocompletionHelper"

  def replace(inputString: String,
              cursorPosition: Int,
              subPathOnly: Boolean = false): PathToReplace = {
    PartialSourcePathAutocompletionHelper.pathToReplace(PartialSourcePathAutoCompletionRequest(inputString, cursorPosition, None, None, None), subPathOnly)(Prefixes.empty)
  }

  def replace(inputString: String): PathToReplace = replace(inputString, inputString.length)

  it should "replace simple multi word input paths" in {
    def replaceFull(input: String) = replace(input, input.length)
    replaceFull("some test") mustBe PathToReplace(0, 9, Some("some test"))
    replace("some test", 3) mustBe PathToReplace(0, 9, Some("some test"))
  }

  it should "replace multi word queries in the middle of a path" in {
    def replaceFull(input: String) = replace(input, input.length)
    replace("a/some test/b", cursorPosition = 2) mustBe PathToReplace(1, 12, Some("some test"))
    replace("a/some test/b", "a/some t".length) mustBe PathToReplace(1, 12, Some("some test"))
  }

  it should "correctly find out which part of a path to replace in simple forward paths for the sub path the cursor is in" in {
    val input = "a1/b1/c1"
    replace(input, 4, subPathOnly = true) mustBe PathToReplace(2, 3, Some("b1"))
    replace(input, 5, subPathOnly = true) mustBe PathToReplace(2, 3, Some("b1"))
    replace(input, 2, subPathOnly = true) mustBe PathToReplace(0, 2, Some("a1"))
  }

  it should "correctly find out which part of a path to replace in simple forward paths for the path suffix the cursor is in" in {
    val input = "a1/b1/c1"
    replace(input, 4) mustBe PathToReplace(2, 6, Some("b1"))
    replace(input, 5) mustBe PathToReplace(2, 6, Some("b1"))
    replace(input, 2) mustBe PathToReplace(0, 8, Some("a1"))
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
    replace(inputString, inputString.length) mustBe
      PathToReplace("a/b[@lang  =  \"en\"]".length, "/error now".length, Some("error now"))
    replace(inputString, inputString.length - 5) mustBe
      PathToReplace("a/b[@lang  =  \"en\"]".length, "/error now".length, Some("error now"))
    replace(inputString2, "a/b[c".length) mustBe
      PathToReplace("a/b[".length, 1, Some("c"), insideFilter = true)
  }

  it should "know if the cursor is inside a filter expression" in {
    val path = """department[id = "department X"]/tags[id"""
    replace(path, path.length).insideFilter mustBe true
  }

  it should "handle URIs correctly" in {
    val pathWithUri = "<https://ns.eccenca.com/source/address>"
    val expectedQuery = Some("address")
    replace(pathWithUri, pathWithUri.length) mustBe PathToReplace(0, pathWithUri.length, query = expectedQuery)
    replace("/" + pathWithUri, pathWithUri.length - 3) mustBe PathToReplace(0, pathWithUri.length + 1, query = expectedQuery, insideUri = true)
    replace("/" + pathWithUri, 0, subPathOnly = true) mustBe PathToReplace(0, 0, query = Some(""))
    replace("/" + pathWithUri, 0, subPathOnly = false) mustBe PathToReplace(0, pathWithUri.length + 1, query = Some(""))
    replace(pathWithUri, pathWithUri.length - 3) mustBe PathToReplace(0, pathWithUri.length, query = expectedQuery, insideUri = true)
    replace(pathWithUri, 0) mustBe PathToReplace(0, pathWithUri.length, query = expectedQuery)
    replace(pathWithUri, 2) mustBe PathToReplace(0, pathWithUri.length, query = expectedQuery, insideUri = true)
    replace(pathWithUri, 1) mustBe PathToReplace(0, pathWithUri.length, query = expectedQuery, insideUri = true)
  }

  it should "suggest replacements for inputs containing URIs correctly" in {
    val input = "<https://ns.eccenca.com/source/address>/rdf:type"
    replace(input, input.length, subPathOnly = true) mustBe PathToReplace(
      "<https://ns.eccenca.com/source/address>".length,
      "/rdf:type".length,
      query = Some("type")
    )
  }

  it should "extract filter queries from URIs and qnames correctly" in {
    replace("<https://ns.eccenca.com/source/address>").query mustBe Some("address")
    replace("\\<urn:test:test>/<https://ns.eccenca.com/source/address>").query mustBe Some("address")
    replace("\\<urn:test:prop>").query mustBe Some("prop")
    replace("<https://eccenca.com/hashProp#prop>").query mustBe Some("prop")
  }

  it should "not auto-complete values inside quotes" in {
    val pathWithQuotes = """a[b = "some value"]"""
    replace(pathWithQuotes, pathWithQuotes.length - 4) mustBe PathToReplace(pathWithQuotes.length - 4, 0, None, insideQuotes = true, insideFilter = true)
    replace("a[@lang = 'e").insideQuotes mustBe true
  }

  it should "should not suggest anything if path in the beginning is invalid" in {
    // some prefix does not exist
    val qualifiedNamePath = "some:uri/"
    replace(qualifiedNamePath, qualifiedNamePath.length) mustBe PathToReplace(qualifiedNamePath.length, 0, None)
  }
}
