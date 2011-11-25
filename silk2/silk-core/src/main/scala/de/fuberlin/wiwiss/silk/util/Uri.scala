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

package de.fuberlin.wiwiss.silk.util

import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Represents a URI.
 */
class Uri(val uri: String) {
  /**
   * The turtle representation of this Uri.
   *
   * Examples:
   * - dbpedia:Berlin
   * - <http://dbpedia.org/resource/Berlin>
   */
  def toTurtle(implicit prefixes: Prefixes): String = {
    for ((id, namespace) <- prefixes if uri.startsWith(namespace)) {
      return id + ":" + uri.substring(namespace.length)
    }

    "<" + uri + ">"
  }

  override def toString = uri
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
   * @param prefixes The prefixes which will be used to resolve the qualified name
   */
  def fromQualifiedName(qualifiedName: String, prefixes: Prefixes) = {
    new Uri(prefixes.resolve(qualifiedName))
  }

  /**
   * Parses an URI in turtle notation.
   *
   * Examples:
   * - dbpedia:Berlin
   * - <http://dbpedia.org/resource/Berlin>
   */
  def parse(str: String, prefixes: Prefixes = Prefixes.empty) = {
    if (str.startsWith("<")) {
      fromURI(str.substring(1, str.length - 1))
    }
    else {
      fromQualifiedName(str, prefixes)
    }
  }
}
