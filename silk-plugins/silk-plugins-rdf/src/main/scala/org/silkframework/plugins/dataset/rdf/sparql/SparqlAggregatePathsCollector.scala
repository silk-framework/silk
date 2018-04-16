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

package org.silkframework.plugins.dataset.rdf.sparql

import java.util.logging.Logger

import org.silkframework.dataset.rdf.SparqlEndpoint
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.entity.{BackwardOperator, ForwardOperator, Path}
import org.silkframework.util.{Timer, Uri}

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
  def apply(endpoint: SparqlEndpoint, graph: Option[String], restrictions: SparqlRestriction, limit: Option[Int]): IndexedSeq[Path] = {
    val forwardPaths = getForwardPaths(endpoint, graph, restrictions, limit.getOrElse(200))
    val backwardPaths = getBackwardPaths(endpoint, graph, restrictions, 10)

    (forwardPaths ++ backwardPaths).toIndexedSeq.sortBy(-_._2).map(_._1)
  }

  private def getForwardPaths(endpoint: SparqlEndpoint, graph: Option[String], restrictions: SparqlRestriction, limit: Int): Traversable[(Path, Double)] = {
    Timer("Retrieving forward pathes for '" + restrictions + "'") {
      val variable = restrictions.variable

      var sparql = new StringBuilder()
      sparql ++= "SELECT ?p ( count(?" + variable + ") AS ?count ) WHERE {\n"

      for (graphUri <- graph if !graphUri.isEmpty)
        sparql ++= "GRAPH <" + graphUri + "> {\n"

      sparql ++= restrictions.toSparql + "\n"
      sparql ++= "?" + variable + " ?p ?o\n"

      for (graphUri <- graph if !graphUri.isEmpty)
        sparql ++= "}\n"

      sparql ++= "}\n"
      sparql ++= "GROUP BY ?p\n"
      sparql ++= "ORDER BY DESC (?count)"

      val results = endpoint.select(sparql.toString(), limit).bindings.toList
      if (results.nonEmpty) {
        val maxCount = results.head("count").value.toDouble
        for (result <- results if result.contains("p")) yield {
          (Path(ForwardOperator(result("p").value) :: Nil),
            result("count").value.toDouble / maxCount)
        }
      } else {
        Traversable.empty
      }
    }
  }

  private def getBackwardPaths(endpoint: SparqlEndpoint, graph: Option[String], restrictions: SparqlRestriction, limit: Int): Traversable[(Path, Double)] = {
    Timer("Retrieving backward pathes for '" + restrictions + "'") {
      val variable = restrictions.variable

      var sparql = new StringBuilder()
      sparql ++= "SELECT ?p ( count(?" + variable + ") AS ?count ) WHERE {\n"

      for (graphUri <- graph if !graphUri.isEmpty)
        sparql ++= "GRAPH <" + graphUri + "> {\n"

      sparql ++= restrictions.toSparql + "\n"
      sparql ++= "?s ?p ?" + variable + " .\n"
      sparql ++= s"FILTER isIRI(?$variable)\n"

      for (graphUri <- graph if !graphUri.isEmpty)
        sparql ++= "}\n"

      sparql ++= "}\n"
      sparql ++= "GROUP BY ?p\n"
      sparql ++= "ORDER BY DESC (?count)"

      val results = endpoint.select(sparql.toString(), limit).bindings.toList
      if (results.nonEmpty) {
        val maxCount = results.head("count").value.toDouble
        for (result <- results if result.contains("p")) yield {
          (Path(BackwardOperator(result("p").value) :: Nil),
            result("count").value.toDouble / maxCount)
        }
      } else {
        Traversable.empty
      }
    }
  }
}
