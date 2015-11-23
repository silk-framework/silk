package org.silkframework.testutil

import org.scalatest.matchers.{MatchResult, BeMatcher}

/**
 * Matcher to test if 2 strings are equal when ignoring whitespaces.
 */
case class equalIgnoringWhitespace(r: String) extends BeMatcher[String] {

  def apply(l: String) = {
    //Strip both strings from whitespace characters
    val ls = l.filterNot(_.isWhitespace)
    val rs = r.filterNot(_.isWhitespace)
    //Match strings
    MatchResult(
      ls == rs,
      ls + " is not equal to " + rs,
      ls + " is equal to " + rs
    )
  }
}