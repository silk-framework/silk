package controllers.util

/**
  * Utility methods for text based search (APIs).
  */
object TextSearchUtils {
  /** Split text query into multi term search */
  def extractSearchTerms(term: String): Array[String] = {
    term.toLowerCase.split("\\s+").filter(_.nonEmpty)
  }

  /** Match search terms against string. Returns only true if all search terms match at least one of the provided strings. */
  def matchesSearchTerm(lowerCaseSearchTerms: Iterable[String], searchIn: String*): Boolean = {
    matchesSearchTerm(lowerCaseSearchTerms, searchIn, Iterable.empty)
  }

  /** Match search terms against strings.
    * Returns true if every search term either occurs as a substring in one of the `searchIn` values
    * or is exactly equal (case-insensitive) to one of the `exactMatchSearchIn` values.
    */
  def matchesSearchTerm(lowerCaseSearchTerms: Iterable[String], searchIn: Iterable[String], exactMatchSearchIn: Iterable[String]): Boolean = {
    val lowerCaseTexts = searchIn.map(_.toLowerCase)
    val lowerCaseExactMatches = exactMatchSearchIn.map(_.toLowerCase).toSet
    lowerCaseSearchTerms forall (searchTerm => lowerCaseTexts.exists(_.contains(searchTerm)) || lowerCaseExactMatches.contains(searchTerm))
  }
}