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

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.dataset.rdf.SparqlEndpoint
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlRestriction
import de.fuberlin.wiwiss.silk.entity.{BackwardOperator, ForwardOperator, Path}
import de.fuberlin.wiwiss.silk.util.{Timer, Uri}

/**
 * Retrieves the most frequent property paths by issuing a SPARQL 1.1 aggregation query.
 *
 * It is typically slower than SparqlSamplePathsCollector but also more precise.
 *
 * The current implementation has two limitations:
 * - It does only return paths of length 1
 * - It returns a maximum of 100 forward paths and 10 backward paths
 */
object SparqlAggregatePathsCollector extends SparqlPathsCollector {

  private implicit val logger = Logger.getLogger(getClass.getName)

  /**
   * Retrieves a list of properties which are defined on most entities.
   */
  def apply(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Option[Int]): Seq[Path] = {
    val forwardPaths = getForwardPaths(endpoint, restrictions, limit.getOrElse(100))
    val backwardPaths = getBackwardPaths(endpoint, restrictions, 10)

    (forwardPaths ++ backwardPaths).toSeq.sortBy(-_._2).map(_._1)
  }

  private def getForwardPaths(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Int): Traversable[(Path, Double)] = {
    Timer("Retrieving forward pathes for '" + restrictions + "'") {
      val variable = restrictions.variable

      val sparql = "SELECT ?p ( count(?" + variable + ") AS ?count ) WHERE {\n" +
        restrictions.toSparql + "\n" +
        "?" + variable + " ?p ?o\n" +
        "}\n" +
        "GROUP BY ?p\n" +
        "ORDER BY DESC (?count)"

      val results = endpoint.query(sparql, limit).bindings.toList
      if (results.nonEmpty) {
        val maxCount = results.head("count").value.toDouble
        for (result <- results if result.contains("p")) yield {
          (Path(variable, ForwardOperator(Uri.fromURI(result("p").value)) :: Nil),
            result("count").value.toDouble / maxCount)
        }
      } else {
        Traversable.empty
      }
    }
  }

  private def getBackwardPaths(endpoint: SparqlEndpoint, restrictions: SparqlRestriction, limit: Int): Traversable[(Path, Double)] = {
    Timer("Retrieving backward pathes for '" + restrictions + "'") {
      val variable = restrictions.variable

      val sparql = "SELECT ?p ( count(?" + variable + ") AS ?count ) WHERE {\n" +
        restrictions.toSparql + "\n" +
        "?s ?p ?" + variable + "\n" +
        "}\n" +
        "GROUP BY ?p\n" +
        "ORDER BY DESC (?count)"

      val results = endpoint.query(sparql, limit).bindings.toList
      if (!results.isEmpty) {
        val maxCount = results.head("count").value.toDouble
        for (result <- results if result.contains("p")) yield {
          (Path(variable, BackwardOperator(Uri.fromURI(result("p").value)) :: Nil),
            result("count").value.toDouble / maxCount)
        }
      } else {
        Traversable.empty
      }
    }
  }
}
