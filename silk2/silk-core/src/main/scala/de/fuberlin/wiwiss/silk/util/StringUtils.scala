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

package de.fuberlin.wiwiss.silk.util

object StringUtils {
  implicit def toStringUtils(str: String) = new StringUtils(str)

  object IntLiteral {
    def apply(x: Int) = x.toString

    def unapply(x: String): Option[Int] = try {
      Some(x.toInt)
    } catch {
      case _ => None
    }
  }

  object DoubleLiteral {
    def apply(x: Double) = x.toString

    def unapply(x: String): Option[Double] = try {
      Some(x.toDouble)
    } catch {
      case _ => None
    }
  }

  object BooleanLiteral {
    def apply(x: Boolean) = x.toString

    def unapply(x: String): Option[Boolean] = try {
      Some(x.toBoolean)
    } catch {
      case _ => None
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
    str.flatMap(c => if(c.isUpper) " " + c else c.toString).capitalize
  }
}
