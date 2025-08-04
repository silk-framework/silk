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

import java.net.{URI, URLDecoder}
import org.silkframework.config.Prefixes

import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.language.implicitConversions
import scala.util.{Success, Try}

/**
  * Represents a URI-like identifier.
  *
  * Note that this class does not enforce that a given URI is valid according to
  * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>.
  * Call [[isValidUri]] to determine whether an instance represents a valid URI.
  *
  * @param uri The full (and normalized) representation of the URI-like identifier.
  */
case class Uri(uri: String) {

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

  /**
    * Returns the full representation.
    */
  override def toString: String = uri

  /**
    * Generates a Java URI instance if this is a valid URI and fails otherwise.
    */
  def toURI: Try[URI] = Try{new URI(uri)}

  /**
    * extracts either the fragment if available or the last path segment
    * if neither is available => None
    * @return
    */
  def localName: Option[String] = toURI match {
    case Success(u) if u.getFragment != null =>
      Some(u.getFragment)
    case Success(u) if u.getPath != null && u.getPath.nonEmpty =>
      Some(u.getPath.substring(u.getPath.lastIndexOf("/") + 1))
    case Success(u) if u.isOpaque =>
      val part = u.getSchemeSpecificPart
      val splitIndex = math.max(part.lastIndexOf('/'), part.lastIndexOf(':')) + 1
      Some(part.substring(splitIndex))
    case _ => None
  }

  def namespace: Option[String] = {
    val localNameIndex = uri.lastIndexOf('/') max uri.lastIndexOf('#') max uri.lastIndexOf(':')
    if (localNameIndex >= 0) Some(uri.substring(0, localNameIndex + 1)) else None
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
    * Parses a URI from a turtle-like notation.
    *
    * Three notations are supported for representing URIs
    * 1. Prefixed notation: prefix:name
    * 2. Full URI:  <http://dbpedia.org/resource/Berlin>
    * 3. Plain Identifiers: Name
    */
  def parse(str: String, prefixes: Prefixes = Prefixes.empty): Uri = {
    val trimmed = str.trim
    if (trimmed.startsWith("<")) {
      fromString(trimmed.substring(1, trimmed.length - 1))
    } else if (!trimmed.contains(':')) {
      fromString(trimmed)
    } else if (trimmed.toLowerCase.startsWith("http") || trimmed.toLowerCase.startsWith("urn:")) {
      fromString(trimmed)
    } else {
      fromQualifiedName(trimmed, prefixes)
    }
  }

  /** Extracts a label from the URI. */
  def urlDecodedLocalNameOfURI(uri: Uri): String = {
    urlDecodedLocalNameOfURI(uri.uri)
  }

  /** Extracts a label from the URI. */
  def urlDecodedLocalNameOfURI(uri: String): String = {
    val slashIndex = uri.lastIndexOf('/')
    val hashIndex = uri.lastIndexOf('#')
    val colonIndex = uri.lastIndexOf(':')
    var cutIndex = if (hashIndex > slashIndex) hashIndex else slashIndex
    cutIndex = if(colonIndex > cutIndex) colonIndex else cutIndex
    val localName = uri.substring(cutIndex + 1, uri.length)
    URLDecoder.decode(localName, "UTF-8")
  }

  /**
   * Generates a name based UUID URI.
   */
  def uuid(value: String): Uri = {
    "urn:uuid:" + UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8))
  }

  /**
   * Generates a random UUID URI.
   */
  def randomUuid: Uri = {
    "urn:uuid:" + UUID.randomUUID().toString
  }
}
