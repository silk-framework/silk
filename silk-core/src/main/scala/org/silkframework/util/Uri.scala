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

import java.net.{URI, URISyntaxException}

import org.silkframework.config.Prefixes

import scala.language.implicitConversions

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
    if (!uri.contains(':')) {
      uri
    } else {
      for ((id, namespace) <- prefixes if uri.startsWith(namespace)) {
        return id + ":" + uri.substring(namespace.length)
      }
      "<" + uri + ">"
    }
  }

  /**
    * Checks if this is a valid URI according to:
    * <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC&nbsp;2732</a>.
    * Only accepts absolute URIs.
    */
  def isValidUri: Boolean = {
    try {
      val u = new URI(uri)
      u.isAbsolute
    } catch {
      case _: URISyntaxException => false
    }
  }

  override def toString: String = uri
}

object Uri {
  /**
    * Builds a URI from a string.
    */
  implicit def fromURI(uri: String): Uri = {
    new Uri(uri)
  }

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
      fromURI(str.substring(1, str.length - 1))
    } else if (!str.contains(':')) {
      fromURI(str)
    } else if (str.startsWith("http")) {
      fromURI(str)
    } else {
      fromQualifiedName(str, prefixes)
    }
  }
}
