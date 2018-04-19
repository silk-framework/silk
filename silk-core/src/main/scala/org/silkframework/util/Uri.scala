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

import java.net.URI

import org.silkframework.config.Prefixes

import scala.language.implicitConversions
import scala.util.{Success, Try}

/**
  * Represents a URI.
  *
  * Three notations are supported for representing URIs
  * 1. Prefixed notation: prefix:name
  * 2. Full URI:  <http://dbpedia.org/resource/Berlin>
  * 3. Plain Identifiers: Name
  *
  * Note that this class does not enforce that a given URI is valid according to
  * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>.
  * Call [[isValidUri]] to determine whether an instance represents a valid URI.
  */
case class Uri(private val u: String) {

  lazy val uri: String = if(u.trim.startsWith("<") && u.trim.endsWith(">"))
    u.trim.substring(1, u.trim.length - 1)
  else
    u.trim

  /**
    * A turtle-like representation of this URI.
    *
    * Examples:
    * - dbpedia:Berlin
    * - <http://dbpedia.org/resource/Berlin>
    * - someName
    */
  def serialize(implicit prefixes: Prefixes): String = {
    if(isValidUri) {
      prefixes.flatMap(p => if (uriMatchesNamespace(uri, p._2)) Some(p._1) else None).headOption match {
        case Some(prefix) => prefix + ":" + uri.substring(prefixes(prefix).length)
        case None => "<" + uri + ">"
      }
    }
    else {
      uri
    }
  }

  private def uriMatchesNamespace(uri: String, namespace: String): Boolean = {
    uri.startsWith(namespace) && {
      val localPart = uri.drop(namespace.length)
      localPart.nonEmpty &&
          !localPart.contains("/") &&
          !localPart.contains("#")
    }
  }

  /**
    * Checks if this is a valid URI according to:
    * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>.
    * Only accepts absolute URIs.
    */
  def isValidUri: Boolean = toURI match{
    case Success(u) if u.isAbsolute => true
    case _ => false
  }

  override def toString: String = uri

  def toURI: Try[URI] = Try{new URI(uri)}

  /**
    * extracts either the fragment if available or the last path segment
    * if neither is available => None
    * @return
    */
  def localName: Option[String] = toURI match{
    case Success(u) if u.getFragment != null                    => Some(u.getFragment)
    case Success(u) if u.getPath != null && u.getPath.nonEmpty  => Some(u.getPath.substring(u.getPath.lastIndexOf("/") + 1))
    case _ => None
  }
}

object Uri {
  /**
    * Builds a URI from a string.
    */
  implicit def fromString(uri: String): Uri = new Uri(uri)

  implicit def asString(uri: Uri): String = uri.toString

  /**
    * Builds a URI from a qualified name.
    *
    * @param qualifiedName The qualified name e.g. dbpedia:Berlin
    * @param prefixes      The prefixes which will be used to resolve the qualified name
    */
  def fromQualifiedName(qualifiedName: String, prefixes: Prefixes): Uri = {
    new Uri(prefixes.resolve(qualifiedName))
  }

  /**
    * Parses an URI in turtle-like notation.
    *
    * Examples:
    * - dbpedia:Berlin
    * - <http://dbpedia.org/resource/Berlin>
    * - someName
    */
  def parse(str: String, prefixes: Prefixes = Prefixes.empty): Uri = {
    if (str.startsWith("<")) {
      fromString(str.substring(1, str.length - 1))
    } else if (!str.contains(':')) {
      fromString(str)
    } else if (str.startsWith("http")) {
      fromString(str)
    } else {
      fromQualifiedName(str, prefixes)
    }
  }
}
