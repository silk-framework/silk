package org.silkframework.rule.expressions

import scala.util.matching.Regex

object ReservedCharacters {

  // All reserved characters
  val all = Set('[', ']', '(', ')', ':', ';')

  // Character used to escape reserved characters
  val escapeCharacter = '\\'

  /**
    * A regex that matches non-empty values in which all reserved characters are escaped.
    */
  val escapedValueRegex: Regex = {
    val escapedCharRegex = Regex.quote(escapeCharacter.toString) + "[" + Regex.quote(all.mkString("")) + "]"
    val legalCharRegex = "[^" + Regex.quote(all.mkString("")) + "]"

    s"(?:$escapedCharRegex|$legalCharRegex)+".r
  }

  def escape(value: String): String = {
    val sb = new StringBuilder()
    for(c <- value) {
      if(all.contains(c)) {
        sb += escapeCharacter
      }
      sb += c
    }
    sb.toString
  }

  def unescape(value: String): String = {
    value.filterNot(_ == escapeCharacter)
  }

}
