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

package org.silkframework.entity.rdf

import org.silkframework.config.Prefixes
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Uri

import scala.util.matching.Regex

/**
 * Represents a SPARQL restriction.
 */
class SparqlRestriction private(val variable: String, restrictionsFull: String, restrictionsQualified: String) extends Serializable {
  // Check if the restriction contains the variable
  require(restrictionsQualified.trim.isEmpty || restrictionsQualified.contains("?" + variable),
    s"SPARQL restriction '$restrictionsQualified' does not use variable ?$variable and thus has no effect. Use ?$variable for restrictions on entities.")

  def toSparql: String = restrictionsFull

  def toSparqlQualified: String = restrictionsQualified

  def isEmpty: Boolean = restrictionsFull.isEmpty

  def merge(other: SparqlRestriction): SparqlRestriction = {
    require(variable == other.variable, "Variables must match")
    SparqlRestriction.fromSparql(variable, toSparql + "\n" + other.toSparql)
  }

  override def toString: String = restrictionsQualified

  override def equals(other: Any): Boolean = other match {
    case o: SparqlRestriction => toSparql == o.toSparql
    case _ => false
  }

  override def hashCode(): Int = toSparql.hashCode
}

object SparqlRestriction {

  def empty: SparqlRestriction = new SparqlRestriction(SparqlEntitySchema.variable, "", "")

  /** Create restriction from an arbitrary SPARQL pattern. */
  def fromSparql(variable: String, restrictions: String)(implicit prefixes: Prefixes = Prefixes.empty): SparqlRestriction = {
    val strippedRestrictions = restrictions.trim.stripSuffix(".").trim
    val cleanedRestrictions = if (strippedRestrictions.isEmpty) "" else strippedRestrictions + " ."

    val prefixRegex = new Regex("""([\w-]+):([\w.-]+(?:\\/[\w.-]+)*)""")

    // Replace all prefixes with their full URIs
    val restrictionsFull = prefixRegex.replaceAllIn(cleanedRestrictions, replacePrefix _)

    // Replace full URIs with their prefixed names
    var restrictionsQualified = cleanedRestrictions
    for ((id, namespace) <- prefixes.toSeq.sortBy(_._1.length).reverse) {
      restrictionsQualified = restrictionsQualified.replaceAll("<" + namespace + "([^>]+)>", id + ":" + "$1")
    }

    new SparqlRestriction(variable, restrictionsFull, restrictionsQualified)
  }

  private def replacePrefix(m: Regex.Match)(implicit prefixes: Prefixes = Prefixes.empty): String = {
    val prefix = m.group(1)
    val name = m.group(2)
    prefixes.get(prefix) match {
      case Some(namespace) => s"<$namespace$name>"
      case None => throw new BadUserInputException(s"Unknown prefix '$prefix' in SPARQL restriction.")
    }
  }

  /** Restrict entity to a specific type */
  def forType(typeUri: Uri): SparqlRestriction = {
    if(typeUri.uri.isEmpty) {
      empty
    } else {
      fromSparql(SparqlEntitySchema.variable, s"?a a <$typeUri>.")
    }
  }
}