package de.fuberlin.wiwiss.silk.util

import java.util.UUID

/**
 * An identifier.
 * May only contain the following characters: (a - z, A - Z, 0 - 9, _, -)
 */
class Identifier(private val name: String) {
  require(!name.isEmpty, "Identifier must not be empty.")
  require(name.forall(c => (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_') || (c == '-')),
    "An identifier may only contain the following characters (a - z, A - Z, 0 - 9, _, -). The following identifier is not valid: '" + name + "'.")

  /** Returns the identifier itself. */
  override def toString = name

  override def equals(other: Any) = other match {
    case otherId: Identifier => name.equals(otherId.name)
    case str: String => name.equals(str)
    case _ => false
  }

  override def hashCode = name.hashCode
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
  def apply(str: String) = new Identifier(str)

  /**
   * Generates a new random identifier.
   */
  def random = new Identifier(UUID.randomUUID.toString)

  /**
   * Converts a String to an Identifier.
   * Will throw an exception if the given String is no valid Identifier.
   */
  implicit def fromString(str: String) = new Identifier(str)

  /**
   * Converts an identifier to a String.
   */
  implicit def toString(id: Identifier) = id.toString
}
