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

object StringUtils {
  implicit def toStringUtils(str: String): StringUtils = new StringUtils(str)

  val integerNumber = """^\s*[+-]?(?:(?:[1-9][0-9]*)|(?:0))\s*$""".r

  object IntLiteral {
    def apply(x: Int) = x.toString

    def unapply(x: String): Option[Int] = {
      if(x == null) {
        None
      }
      else {
        try {
          Some(x.toInt)
        } catch {
          case _: NumberFormatException => None
        }
      }
    }
  }

  object DoubleLiteral {
    def apply(x: Double) = x.toString

    def unapply(x: String): Option[Double] = {
      if(x == null) {
        None
      }
      else {
        try {
          Some(x.toDouble)
        } catch {
          case _: NumberFormatException => None
        }
      }
    }
  }

  object BooleanLiteral {
    def apply(x: Boolean) = x.toString

    def unapply(x: String): Option[Boolean] = {
      if(x == null) {
        None
      }
      else {
        try {
          Some(x.toBoolean)
        } catch {
          case _: NumberFormatException => None
        }
      }
    }
  }

  object XSDDateLiteral {

    private val datatypeFactory = DatatypeFactory.newInstance()

    def apply(x: XMLGregorianCalendar) = x.toString

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
   * Undos all camel case words in this string.
   * e.g. helloWorld is converted to "Hello World"
   */
  def undoCamelCase = {
    str.flatMap(c => if(c.isUpper) " " + c else c.toString).capitalize.trim
  }

  /**
   * Uncapitalizes a string.
   * e.g. HelloWorld is converted to helloWorld.
   * @return
   */
  def uncapitalize = {
    str.head.toLower + str.tail
  }
}
