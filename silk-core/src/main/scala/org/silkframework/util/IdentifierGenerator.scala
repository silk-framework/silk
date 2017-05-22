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
  def generate(identifier: String): Identifier = synchronized {
    val (name, num) = split(Identifier.fromAllowed(prefix + identifier))
    identifiers.get(name) match {
      case Some(count) =>
        identifiers += ((name, count + 1))
        name + count.toString
      case None =>
        identifiers += ((name, 1))
        name
    }
  }

  /**
    * Adds an existing identifier without changing it.
    */
  def add(identifier: Identifier): Unit = synchronized {
    val (name, num) = split(identifier)
    identifiers.get(name.toString) match {
      case Some(count) =>
        identifiers += ((name, math.max(count, num + 1)))
      case None =>
        identifiers += ((name, num + 1))
    }
  }

  /**
    * Splits an identifier into a name and a number.
    * For instance 'name3' is split into (name, 3)
    */
  private def split(identifier: Identifier): (String, Int) = {
    val str = identifier.toString
    val splitIndex = str.length - str.reverse.indexWhere(!_.isDigit)
    val (name, numStr) = str.splitAt(splitIndex)
    val num = if(numStr.isEmpty) 0 else numStr.toInt
    (name, num)
  }

}
