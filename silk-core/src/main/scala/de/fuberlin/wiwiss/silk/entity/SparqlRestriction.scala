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

package de.fuberlin.wiwiss.silk.entity

import de.fuberlin.wiwiss.silk.config.Prefixes
import util.matching.Regex

/**
 * Represents a SPARQL restriction.
 */
class SparqlRestriction private(val variable: String, restrictionsFull: String, restrictionsQualified: String) {

  def toSparql = restrictionsFull

  def toSparqlQualified = restrictionsQualified

  def isEmpty = restrictionsFull.isEmpty

  override def toString = restrictionsQualified

  override def equals(other: Any) = other match {
    case o: SparqlRestriction => toSparql == o.toSparql
    case _ => false
  }
}

object SparqlRestriction {
  def empty = new SparqlRestriction("x", "", "")

  def fromSparql(variable: String, restrictions: String)(implicit prefixes: Prefixes = Prefixes.empty) = {
    val strippedRestrictions = restrictions.trim.stripSuffix(".").trim
    val cleanedRestrictions = if (strippedRestrictions.isEmpty) "" else strippedRestrictions + " ."

    var restrictionsFull = cleanedRestrictions
    var restrictionsQualified = cleanedRestrictions
    for ((id, namespace) <- prefixes.toSeq.sortBy(_._1.length).reverse) {
      restrictionsFull = restrictionsFull.replaceAll(" " + id + ":" + "([^\\s\\{\\}+*]+)([+*]*\\s+\\.)?", " <" + namespace + "$1>$2")
      restrictionsQualified = restrictionsQualified.replaceAll("<" + namespace + "([^>]+)>", id + ":" + "$1")
    }

    //Check if a prefix is missing
    val missingPrefixes = new Regex("[\\s\\{\\}][^<\\s\\{\\}]+:").findAllIn(restrictionsFull)
    if (!missingPrefixes.isEmpty) {
      throw new IllegalArgumentException("The following prefixes are not defined: " + missingPrefixes.mkString(","))
    }

    new SparqlRestriction(variable, restrictionsFull, restrictionsQualified)
  }
}