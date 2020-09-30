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
  def matchesSearchTerm(lowerCaseSearchTerms: Seq[String], searchIn: String*): Boolean = {
    val lowerCaseTexts = searchIn.map(_.toLowerCase)
    lowerCaseSearchTerms forall (searchTerm => lowerCaseTexts.exists(_.contains(searchTerm)))
  }
}