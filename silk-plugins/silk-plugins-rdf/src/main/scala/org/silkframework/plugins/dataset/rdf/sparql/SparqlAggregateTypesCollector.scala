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
import org.silkframework.util.Timer

object SparqlAggregateTypesCollector extends SparqlTypesCollector {

  private val defaultLimit = 10000

  private implicit val logger = Logger.getLogger(getClass.getName)

  def apply(endpoint: SparqlEndpoint, graph: Option[String], limit: Option[Int]): Traversable[(String, Double)] = {
    Timer("Retrieving types in '" + endpoint + "'") {
      val query = buildQuery(graph)
      val results = endpoint.select(query, limit.getOrElse(defaultLimit)).bindings.toList
      if (results.nonEmpty) {
        val maxCount = results.head("count").value.toDouble
        for (result <- results if result.contains("t")) yield {
          (result("t").value, result("count").value.toDouble / maxCount)
        }
      } else {
        Traversable.empty
      }
    }
  }

  private def buildQuery(graph: Option[String]): String = {
    var sparql = "SELECT ?t (count(?t) AS ?count) WHERE {\n"

    for (graphUri <- graph if !graphUri.isEmpty)
      sparql += "GRAPH <" + graphUri + "> {\n"

    sparql += "?s a ?t\n"

    for (graphUri <- graph if !graphUri.isEmpty)
      sparql += "}\n"

    sparql += "}\n"
    sparql += "GROUP BY ?t\n"
    sparql += "ORDER BY DESC (?count)"

    sparql
  }
}
