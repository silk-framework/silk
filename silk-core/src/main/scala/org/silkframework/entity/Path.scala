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

import java.net.URLEncoder

import org.silkframework.config.Prefixes
import org.silkframework.util.Uri

import scala.ref.WeakReference
import scala.util.Try

/**
  * Represents an RDF path.
  */
class Path private[entity](val operators: List[PathOperator]) extends Serializable {

  /**
    * The normalized serialization using the Silk RDF path language.
    * Guaranties that the following equivalence holds true: path1 == path2 <=> path1.normalizedSerialization == normalizedSerialization
    */
  lazy val normalizedSerialization: String = serializePath(Prefixes.empty, stripForwardSlash = true)

  /**
    * Serializes this path using the Silk RDF path language.
    *
    * @param stripForwardSlash If true and if the path beginns with a forward operator, the first forward slash is stripped.
    * @param prefixes The prefixes used to shorten the path. If no prefixes are provided the normalized serialization is returned.
    */
  def serialize(stripForwardSlash: Boolean = true)(implicit prefixes: Prefixes = Prefixes.empty): String = prefixes match {
    case Prefixes.empty if stripForwardSlash => normalizedSerialization
    case _ => serializePath(prefixes, stripForwardSlash)
  }

  /**
    * Internal path serialization function.
    */
  private def serializePath(prefixes: Prefixes, stripForwardSlash: Boolean): String = {
    val pathStr = operators.map(_.serialize(prefixes)).mkString
    if(stripForwardSlash) {
      pathStr.stripPrefix("/")
    } else {
      pathStr
    }
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
    * Returns the number of operators in this path.
    */
  def size: Int = operators.size

  /**
    * Tests if this path is empty, i.e, has not operators.
    */
  def isEmpty: Boolean = operators.isEmpty

  /**
    * Concatenates this path with another path.
    */
  def ++(path: Path): Path = Path(operators ::: path.operators)

  override def toString: String = normalizedSerialization

  /**
    * Tests if this path equals another path
    */
  override def equals(other: Any): Boolean = {
    other match {
      case p: Path => normalizedSerialization == p.normalizedSerialization
      case _ => false
    }
  }

  override def hashCode: Int = normalizedSerialization.hashCode

  /** Returns a [[org.silkframework.entity.TypedPath]] from this path with string type values. */
  def asStringTypedPath: TypedPath = TypedPath(this.operators, StringValueType, isAttribute = false)

  /** Returns a [[org.silkframework.entity.TypedPath]] from this path with auto detect type. */
  def asAutoDetectTypedPath: TypedPath = TypedPath(this.operators, AutoDetectValueType, isAttribute = false)
}

object Path {

  private var pathCache = Map[String, WeakReference[Path]]()

  def empty: Path = new Path(List.empty)

  /**
    * Creates a new path.
    * Returns a cached copy if available.
    */
  def apply(operators: List[PathOperator]): Path = {
    val path = new Path(operators)

    val pathStr = path.serialize()

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
    var uri = Uri.parse(property)
    if(!uri.isValidUri && !Uri("http://ex.org/" + property).isValidUri)
      uri = Uri(URLEncoder.encode(property, "UTF-8"))
    apply(ForwardOperator(uri) :: Nil)
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