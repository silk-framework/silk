package de.fuberlin.wiwiss.silk.util

/**
 * An identifier.
 * May only contain the following characters: (a - z, 0 - 9, Space, _)
 */
class Identifier(private val name : String)
{
  require(!name.isEmpty, "Identifier must not be empty.")
  require(name.forall(c => (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == ' ') || (c == '_')),
          "An identifier may only contain the following characters (a - z, 0 - 9, Space, _). The following identifier is not valid: '" + name + "'.")

  override def toString = name

  override def equals(other : Any) = other match
  {
    case otherId : Identifier => name.equals(otherId.name)
    case str : String => name.equals(str)
    case _ => false
  }

  override def hashCode = name.hashCode
}

object Identifier
{
  implicit def fromString(str : String) = new Identifier(str)

  implicit def toString(id : Identifier) = id.toString
}
