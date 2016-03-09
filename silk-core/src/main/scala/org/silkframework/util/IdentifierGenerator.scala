package org.silkframework.util

/**
  * Generates identifiers that are unique in its scope.
  */
class IdentifierGenerator(prefix: String = "") {

  /**
    * Remembers all existing identifiers and their counts.
    */
  private var identifiers = Map[String, Int]()

  /**
    * Generates a new unique identifier.
    */
  def generate(name: String): Identifier = synchronized {
    val identifier = Identifier.fromAllowed(prefix + name)

    identifiers.get(identifier.toString) match {
      case Some(count) =>
        identifiers += ((identifier.toString, count + 1))
        identifier + count.toString
      case None =>
        identifiers += ((identifier.toString, 1))
        identifier
    }
  }

}
