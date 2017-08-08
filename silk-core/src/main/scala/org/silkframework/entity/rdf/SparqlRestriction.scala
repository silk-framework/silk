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
import org.silkframework.util.Uri

import scala.util.matching.Regex

/**
 * Represents a SPARQL restriction.
 */
class SparqlRestriction private(val variable: String, restrictionsFull: String, restrictionsQualified: String) extends Serializable {
  // Check if the restriction contains the variable
  require(restrictionsQualified.trim.isEmpty || restrictionsQualified.contains("?" + variable),
    s"SPARQL restriction '$restrictionsQualified' does not use variable ?$variable and thus has no effect. Use ?$variable for restrictions on entities.")

  def toSparql = restrictionsFull

  def toSparqlQualified = restrictionsQualified

  def isEmpty = restrictionsFull.isEmpty

  def merge(other: SparqlRestriction) = {
    require(variable == other.variable, "Variables must match")
    SparqlRestriction.fromSparql(variable, toSparql + "\n" + other.toSparql)
  }

  override def toString = restrictionsQualified

  override def equals(other: Any) = other match {
    case o: SparqlRestriction => toSparql == o.toSparql
    case _ => false
  }
}

object SparqlRestriction {

  def empty: SparqlRestriction = new SparqlRestriction("a", "", "")

  def fromSparql(variable: String, restrictions: String)(implicit prefixes: Prefixes = Prefixes.empty): SparqlRestriction = {
    val strippedRestrictions = restrictions.trim.stripSuffix(".").trim
    val cleanedRestrictions = if (strippedRestrictions.isEmpty) "" else strippedRestrictions + " ."

    var restrictionsFull = cleanedRestrictions
    var restrictionsQualified = cleanedRestrictions
    for ((id, namespace) <- prefixes.toSeq.sortBy(_._1.length).reverse) {
      // Replace prefixes in properties and types
      restrictionsFull = restrictionsFull.replaceAll("([\\s^])" + id + ":" + "([^\\s\\{\\}+*]+)([+*]*\\s+\\.)?", "$1<" + namespace + "$2>$3")
      restrictionsQualified = restrictionsQualified.replaceAll("<" + namespace + "([^>]+)>", id + ":" + "$1")
    }

    //Check if a prefix is missing
    val missingPrefixes = new Regex("[\\s\\{\\}][^<\\s\\{\\}\"]+:").findAllIn(restrictionsFull)
    if (missingPrefixes.nonEmpty) {
      throw new IllegalArgumentException("The following prefixes are not defined: " + missingPrefixes.mkString(","))
    }

    new SparqlRestriction(variable, restrictionsFull, restrictionsQualified)
  }

  def forType(typeUri: Uri): SparqlRestriction = {
    if(typeUri.uri.isEmpty) {
      empty
    } else {
      fromSparql("a", s"?a a <$typeUri>.")
    }
  }
}