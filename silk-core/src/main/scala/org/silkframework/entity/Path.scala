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

package org.silkframework.entity

import org.silkframework.config.Prefixes
import org.silkframework.util.Uri

import scala.ref.WeakReference

/**
  * Represents an RDF path.
  */
class Path private[entity](val operators: List[PathOperator]) extends Serializable {

  private lazy val serializedFull = serialize()

  /**
    * Serializes this path using the Silk RDF path language.
    */
  def serialize(implicit prefixes: Prefixes = Prefixes.empty): String = operators.map(_.serialize).mkString

  /**
    * Serializes this path using the simplified notation.
    */
  def serializeSimplified(implicit prefixes: Prefixes = Prefixes.empty): String = {
    operators.map(_.serialize).mkString.stripPrefix("/")
  }

  /**
    * Returns the property URI, if this is a simple forward path of length 1.
    * Otherwise, returns none.
    */
  def propertyUri: Option[Uri] = operators match {
    case ForwardOperator(prop) :: Nil => Some(prop)
    case _ => None
  }

  /**
    * extracts either the fragment if available or the last path segment
    * if neither is available => None
    * @return
    */
  def getLocalName: Option[String] = propertyUri.flatMap(_.localName )

  def size: Int = operators.size

  /**
    * Tests if this path is empty, i.e, has not operators.
    */
  def isEmpty: Boolean = operators.isEmpty

  /**
    * Concatenates this path with another path.
    */
  def ++(path: Path): Path = Path(operators ::: path.operators)

  override def toString: String = serializedFull

  /**
    * Tests if this path equals another path
    */
  override def equals(other: Any): Boolean = {
    //Because of the path cache it is sufficient to compare by reference
    //    other match {
    //      case otherPath: Path => this eq otherPath
    //      case _ => false
    //    }
    // As paths are serializable now, comparing by reference no longer suffices
    other match {
      case p: Path => serializedFull == p.serializedFull
      case _ => false
    }

  }

  override def hashCode: Int = toString.hashCode

  /** Returns a [[org.silkframework.entity.TypedPath]] from this path with string type values. */
  def asStringTypedPath: TypedPath = TypedPath(this.operators, StringValueType, isAttribute = false)
}

object Path {
  private var pathCache = Map[String, WeakReference[Path]]()

  def empty = new Path(List.empty)

  /**
    * Creates a new path.
    * Returns a cached copy if available.
    */
  def apply(operators: List[PathOperator]): Path = {
    val path = new Path(operators)

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

  def unapply(path: Path): Option[List[PathOperator]] = {
    Some(path.operators)
  }

  /**
    * Creates a path consisting of a single property
    */
  def apply(property: String): Path = {
    apply(ForwardOperator(property) :: Nil)
  }

  /**
    * Creates a path consisting of a single property
    */
  def apply(property: Uri): Path = {
    apply(property.uri)
  }

  /**
    * Parses a path string.
    * Returns a cached copy if available.
    */
  def parse(pathStr: String)(implicit prefixes: Prefixes = Prefixes.empty): Path = {
    new PathParser(prefixes).parse(pathStr)
  }
}