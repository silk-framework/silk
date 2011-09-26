package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.config.Prefixes
import ref.WeakReference
/**
 * Represents an RDF path.
 */
final class Path private(val variable: String, val operators: List[PathOperator]) {
  /**
   * Serializes this path using the Silk RDF path language.
   */
  def serialize(implicit prefixes: Prefixes = Prefixes.empty) = "?" + variable + operators.map(_.serialize).mkString

  override def toString = serialize(Prefixes.empty)

  /**
   * Tests if this path equals another path
   */
  override def equals(other: Any) = {
    //Because of the path cache it is sufficient to compare by reference
    other match {
      case otherPath: Path => this eq otherPath
      case _ => false
    }
  }

  override def hashCode = toString.hashCode
}

object Path {
  private var pathCache = Map[String, WeakReference[Path]]()

  /**
   * Creates a new path.
   * Returns a cached copy if available.
   */
  def apply(variable: String, operators: List[PathOperator]): Path = {
    val path = new Path(variable, operators)

    val pathStr = path.serialize

    //Remove all garbage collected paths from the map and try to return a cached path
    synchronized {
      pathCache = pathCache.filter(_._2.get.isDefined)

      pathCache.get(pathStr).flatMap(_.get) match {
        case Some(cachedPath) => cachedPath
        case None => {
          pathCache += (pathStr -> new WeakReference(path))
          path
        }
      }
    }
  }

  /**
   * Parses a path string.
   * Returns a cached copy if available.
   */
  def parse(pathStr: String)(implicit prefixes: Prefixes = Prefixes.empty): Path = {
    new PathParser(prefixes).parse(pathStr)
  }
}
