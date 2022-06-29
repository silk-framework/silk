/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.util

import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import scala.language.implicitConversions
import scala.util.Try
import scala.util.matching.Regex

object StringUtils {
  implicit def toStringUtils(str: String): StringUtils = new StringUtils(str)

  val integerNumber: Regex = """^\s*[+-]?(?:(?:[1-9][0-9]*)|(?:0))\s*$""".r
  val simpleDoubleNumber: Regex =
    ("""^\s*[+-]?""" + // sign
        """(?:[1-9][0-9]*|0)?""" + // whole numbers
        """(?:""" +
          """\.[0-9]+(?:[eE][-+]?[0-9]+)?""" + // either with point, but optional exponent
          """|""" +
          """[eE][-+]?[0-9]+""" + // or without, but obligatory exponent
        """)""" +
        """\s*$""").
      replaceAllLiterally("\n", "").
      replaceAll("\r", "").
      r

  object IntLiteral {
    def apply(x: Int): String = x.toString

    def unapply(x: String): Option[Int] = {
      try {
        Some(x.toInt)
      } catch {
        case _: NumberFormatException =>
          None
      }
    }
  }

  object DoubleLiteral {

    def isDouble(str: String): Boolean = {
      try {
        str.toDouble
        true
      } catch {
        case _: NumberFormatException =>
          false
      }
    }

    def apply(x: Double): String = x.toString

    def unapply(x: String): Option[Double] = {
      try {
        Some(x.toDouble)
      } catch {
        case _: NumberFormatException =>
          None
      }
    }
  }

  object BooleanLiteral {
    def apply(x: Boolean): String = x.toString

    def unapply(x: String): Option[Boolean] = {
      Option(x) flatMap { s =>
        Try(s.toBoolean).toOption
      }
    }
  }

  object XSDDateLiteral {

    private val datatypeFactory = DatatypeFactory.newInstance()

    def apply(x: XMLGregorianCalendar): String = x.toString

    def unapply(x: String): Option[XMLGregorianCalendar] = {
      try {
        Some(datatypeFactory.newXMLGregorianCalendar(x))
      } catch {
        case _: NullPointerException => None
        case _: IllegalArgumentException => None
      }
    }
  }

  /** Match search terms against string. Returns only true if all search terms match. */
  def matchesSearchTerm(lowerCaseSearchTerms: Seq[String],
                        searchIn: String,
                        convertTextToLowercase: Boolean = true): Boolean = {
    if(lowerCaseSearchTerms.isEmpty) {
      true
    } else {
      val lowerCaseText = if(convertTextToLowercase) searchIn.toLowerCase else searchIn
      lowerCaseSearchTerms forall lowerCaseText.contains
    }
  }

  /** Counts how many search words match the text. Search text is expected to be in lower case. */
  def matchCount(lowerCaseSearchTerms: Seq[String],
                 searchIn: String): Int = {
    if(lowerCaseSearchTerms.isEmpty) {
      0
    } else {
      lowerCaseSearchTerms.map(term => if(searchIn.contains(term)) 1 else 0).sum
    }
  }

  /** Split text query into multi lower case term search */
  def extractSearchTerms(term: String): Array[String] = {
    term.toLowerCase.split("\\s+").filter(_.nonEmpty)
  }
}

class StringUtils(str: String) {

  /**
    * Returns a stream of all q-grams in this string.
    */
  def qGrams(q: Int): Stream[String] = {
    val boundary = "#" * (q - 1)

    (boundary + str + boundary).sliding(q).toStream
  }

  /**
    * Converts a string to upper camel case.
    * e.g. "Hello World" is converted to HelloWorld
    * @return
    */
  def upperCamelCase: String = {
    str.split("\\s").map(_.capitalize).mkString("").trim
  }

  /**
    * Converts a string to sentence case.
    * Undos camel case, e.g., helloWorld is converted to "Hello world"
    */
  def toSentenceCase: String = {
    if(str.isEmpty) {
      return str
    }
    val sb = new StringBuilder()
    sb += str.charAt(0).toUpper
    for(i <- 1 until str.length) {
      val prevChar: Char = str.charAt(i - 1)
      val curChar = str.charAt(i)
      val nextIsUpper = i + 1 < str.length && str.charAt(i + 1).isUpper
      val nextIsLower = i + 1 < str.length && str.charAt(i + 1).isLower
      if(prevChar.isSpaceChar) {
        if(i == 0 || (curChar.isUpper && nextIsUpper)) {
          sb += curChar.toUpper
        } else {
          sb += curChar.toLower
        }
      } else if(prevChar.isLower && curChar.isUpper) {
        sb += ' '
        if(nextIsUpper) {
          sb += curChar
        } else {
          sb += curChar.toLower
        }
      } else if(prevChar.isUpper && curChar.isUpper && nextIsLower) {
        sb += ' '
        sb += curChar.toLower
      } else {
        sb += curChar
      }
    }
    sb.toString
  }

  /**
    * Uncapitalizes a string.
    * e.g. HelloWorld is converted to helloWorld.
    *
    * @return
    */
  def uncapitalize: String = {
    str.head.toLower + str.tail
  }
}
