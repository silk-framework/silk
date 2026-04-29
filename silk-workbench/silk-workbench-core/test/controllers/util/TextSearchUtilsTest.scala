package controllers.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class TextSearchUtilsTest extends AnyFlatSpec with Matchers {
  behavior of "TextSearchUtils.matchesSearchTerm"

  private def terms(s: String): Array[String] = TextSearchUtils.extractSearchTerms(s)

  it should "match search terms via substring against searchIn (legacy varargs overload)" in {
    TextSearchUtils.matchesSearchTerm(terms("foo"), "Hello FooBar World") mustBe true
    TextSearchUtils.matchesSearchTerm(terms("foo bar"), "fooworld", "barworld") mustBe true
    TextSearchUtils.matchesSearchTerm(terms("missing"), "Hello FooBar World") mustBe false
  }

  it should "match a search term that is exactly equal to an exact-match value (case-insensitive)" in {
    TextSearchUtils.matchesSearchTerm(terms("secret"), Seq("Hello World"), Seq("secret")) mustBe true
    TextSearchUtils.matchesSearchTerm(terms("Secret"), Seq("Hello World"), Seq("SECRET")) mustBe true
  }

  it should "NOT match a search term that is only a substring of an exact-match value" in {
    TextSearchUtils.matchesSearchTerm(terms("sec"), Seq("Hello World"), Seq("secret")) mustBe false
    TextSearchUtils.matchesSearchTerm(terms("ret"), Seq("Hello World"), Seq("secret")) mustBe false
  }

  it should "NOT match if a search term contains an exact-match value but is longer" in {
    TextSearchUtils.matchesSearchTerm(terms("secrets"), Seq("Hello World"), Seq("secret")) mustBe false
  }

  it should "satisfy a search term via either substring searchIn or exact-match searchIn" in {
    TextSearchUtils.matchesSearchTerm(terms("foo secret"), Seq("HelloFooBar"), Seq("secret")) mustBe true
    TextSearchUtils.matchesSearchTerm(terms("foo secret"), Seq("HelloFooBar"), Seq("sec")) mustBe false
    TextSearchUtils.matchesSearchTerm(terms("foo missing"), Seq("HelloFooBar"), Seq("secret")) mustBe false
  }

  it should "require all search terms to be satisfied" in {
    TextSearchUtils.matchesSearchTerm(terms("alpha beta"), Seq("alphabet"), Seq("beta")) mustBe true
    TextSearchUtils.matchesSearchTerm(terms("alpha gamma"), Seq("alphabet"), Seq("beta")) mustBe false
  }

  it should "treat empty exactMatchSearchIn as the legacy substring-only behavior" in {
    TextSearchUtils.matchesSearchTerm(terms("foo"), Seq("HelloFooBar"), Iterable.empty) mustBe true
    TextSearchUtils.matchesSearchTerm(terms("missing"), Seq("HelloFooBar"), Iterable.empty) mustBe false
  }

  it should "return true for an empty search-term list" in {
    TextSearchUtils.matchesSearchTerm(terms(""), Seq("anything"), Seq("anything")) mustBe true
    TextSearchUtils.matchesSearchTerm(terms("   "), Seq("anything"), Seq("anything")) mustBe true
  }
}