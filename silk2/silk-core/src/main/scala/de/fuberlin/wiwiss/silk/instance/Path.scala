package de.fuberlin.wiwiss.silk.instance

import collection.mutable.{SynchronizedMap, WeakHashMap}
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Represents an RDF path.
 */
final class Path private(val variable : String, val operators : List[PathOperator])
{
  /**
   * Serializes this path using the Silk RDF path language.
   */
  def serialize(implicit prefixes : Prefixes = Prefixes.empty) = "?" + variable + operators.map(_.serialize).mkString

  override def toString = serialize(Prefixes.empty)

  /**
   * Tests if this path equals another path
   */
  override def equals(other : Any) =
  {
    //Because of the path cache it is sufficient to compare by reference
    other match
    {
      case otherPath : Path => this eq otherPath
      case _ => false
    }
  }

  override def hashCode = toString.hashCode
}

object Path
{
  private val pathCache = new WeakHashMap[String, Path]() with SynchronizedMap[String, Path]

  /**
   * Creates a new path.
   * May return a cached copy.
   */
  def apply(variable : String, operators : List[PathOperator]) : Path =
  {
    val path = new Path(variable, operators)

    pathCache.getOrElseUpdate(path.serialize, path)
  }

  /**
   * Parses a path string.
   * May return a cached copy.
   */
  def parse(pathStr : String)(implicit prefixes : Prefixes = Prefixes.empty) : Path =
  {
    new PathParser(prefixes).parse(pathStr)
  }
}
