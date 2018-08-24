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

package org.silkframework.config

import org.silkframework.runtime.validation.ValidationException

import scala.collection.immutable
import scala.language.implicitConversions
import scala.xml.{Elem, Node}

/**
 * Holds namespace prefixes.
 */
case class Prefixes(prefixMap: immutable.HashMap[String, String]) extends Serializable {

  override def toString: String = "Prefixes(" + prefixMap.toString + ")"

  override def equals(other: Any): Boolean = other match {
    case o: Prefixes => this.prefixMap == o.prefixMap
    case _ => false
  }

  /**
   * Combines two prefix objects.
   */
  def ++(prefixes: Prefixes): Prefixes = {
    new Prefixes(immutable.HashMap[String, String]((this.prefixMap ++ prefixes.prefixMap).toSeq:_*))
  }

  /**
    * Combines two prefix objects.
    */
  def ++(prefixMap: Map[String, String]): Prefixes = {
    new Prefixes(immutable.HashMap[String, String]((this.prefixMap ++ prefixMap).toSeq:_*))
  }

  /**
   * Resolves a qualified name to its full URI.
   *
   * @param name The qualified name e.g. rdf:label.
   * @return The full URI e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#label
   * @see shorten
   */
  def resolve(name: String): String = name.split(":", 2) match {
    case Array(prefix, suffix) => prefixMap.get(prefix) match {
      case Some(resolvedPrefix) => resolvedPrefix + suffix
      case None => throw new ValidationException(
        s"Unknown prefix: '$prefix'. Please add the missing prefix to the project " +
         "or use a full URI, e.g., <http://example.org/name>.")
    }
    case _ => throw new ValidationException(
      s"Expected a prefixed name of the form 'prefix:name', but got '$name'. " +
       "If you want to write a full URI, use angle brackets, e.g., <http://example.org/name>.")
  }

  /**
   * Tries to shorten a full URI.
   *
   * @param uri The full URI e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#label
   * @return The qualified name if a prefix was found e.g. rdf:label. The full URI otherwise.
   * @see resolve
   */
  def shorten(uri: String): String = {
    for ((id, namespace) <- prefixMap if uri.startsWith(namespace)) {
      return id + ":" + uri.substring(namespace.length)
    }

    uri
  }

  def toXML: Elem = {
    <Prefixes>
      {for ((key, value) <- prefixMap) yield {
        <Prefix id={key} namespace={value}/>
    }}
    </Prefixes>
  }

  def toSparql: String = {
    var sparql = ""
    for ((key, value) <- prefixMap) {
      sparql += "PREFIX " + key + ": <" + value + "> "
    }
    sparql
  }

}

object Prefixes {

  def apply(map: Map[String, String]): Prefixes = new Prefixes(immutable.HashMap[String, String](map.toSeq:_*))

  val empty = new Prefixes(immutable.HashMap[String, String]())

  val default: Prefixes = {
    new Prefixes(immutable.HashMap[String, String](
      "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
      "owl" -> "http://www.w3.org/2002/07/owl#" ))
  }

  implicit def fromMap(map: Map[String, String]): Prefixes = apply(map)

  implicit def toMap(prefixes: Prefixes): Map[String, String] = prefixes.prefixMap

  def fromXML(xml: Node): Prefixes = {
    new Prefixes(immutable.HashMap[String, String]((xml \ "Prefix").map(n => ((n \ "@id").text, (n \ "@namespace").text)):_*))
  }
}
