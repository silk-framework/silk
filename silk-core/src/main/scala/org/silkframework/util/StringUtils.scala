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
      Option(x) flatMap { s =>
        Try(s.toInt).toOption
      }
    }
  }

  object DoubleLiteral {
    def apply(x: Double): String = x.toString

    def unapply(x: String): Option[Double] = {
      Option(x) flatMap { s =>
        Try(s.toDouble).toOption
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
    * Undos all camel case words in this string.
    * e.g. helloWorld is converted to "Hello World"
    */
  def undoCamelCase: String = {
    str.flatMap(c => if (c.isUpper) " " + c else c.toString).capitalize.trim
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
