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

import java.util.UUID

import scala.language.implicitConversions

/**
 * An identifier.
 * May only contain the following characters: (a - z, A - Z, 0 - 9, _, -)
 */
class Identifier(private val name: String) extends Serializable with Ordered[Identifier] {
  require(!name.isEmpty, "Identifier must not be empty.")
  require(name.forall(Identifier.isAllowed),
    "An identifier may only contain the following characters (a - z, A - Z, 0 - 9, _, -). The following identifier is not valid: '" + name + "'.")

  /** Returns the identifier itself. */
  override def toString: String = name

  override def equals(other: Any): Boolean = other match {
    case otherId: Identifier => name.equals(otherId.name)
    case str: String => name.equals(str)
    case _ => false
  }

  override def hashCode: Int = name.hashCode

  override def compare(that: Identifier): Int = {
    name.compare(that.name)
  }

  def +(other: Any): Identifier = new Identifier(name + other.toString)
}

/**
 * Identifier companion object.
 * Defines conversions between Identifiers and Strings.
 */
object Identifier {
  /**
   * Creates a new Identifier.
   * Will throw an exception if the given String is no valid Identifier.
   */
  def apply(str: String): Identifier = new Identifier(str)

  /**
    * Checks if a given character is valid in identifiers.
    */
  def isAllowed(c: Char): Boolean = {
    (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_') || (c == '-')
  }

  def filterAllowedChars(str: String): String = str.filter(Identifier.isAllowed)

  /**
    * Creates a new Identifier only from the allowed characters in a given string.
    *
    * @param str The input string from which to generate the Identifier
    * @param alternative If the generated ID is empty, the alternative String is used.
    */
  def fromAllowed(str: String, alternative: Option[String] = None): Identifier = {
    try {
      new Identifier(filterAllowedChars(str))
    }  catch {
      case _: IllegalArgumentException if alternative.isDefined =>
        Identifier(alternative.get)
    }
  }

  /**
   * Generates a new random identifier.
   */
  def random: Identifier = new Identifier("r" + UUID.randomUUID.toString)

  /**
   * Converts a String to an Identifier.
   * Will throw an exception if the given String is no valid Identifier.
   */
  implicit def fromString(str: String): Identifier = new Identifier(str)

  /**
   * Converts an identifier to a String.
   */
  implicit def toString(id: Identifier): String = id.toString
}
