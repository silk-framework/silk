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
import de.fuberlin.wiwiss.silk.util.Timer

object SparqlAggregateTypesCollector extends SparqlTypesCollector {

  private val defaultLimit = 10000

  private implicit val logger = Logger.getLogger(getClass.getName)

  def apply(endpoint: SparqlEndpoint, limit: Option[Int]): Traversable[(String, Double)] = {
    Timer("Retrieving types in '" + endpoint + "'") {
      val sparql = "SELECT ?t (count(?t) AS ?count) WHERE {\n" +
        "?s a ?t\n" +
        "}\n" +
        "GROUP BY ?t\n" +
        "ORDER BY DESC (?count)"

      println("QUERY: " + sparql)

      val results = endpoint.query(sparql, limit.getOrElse(defaultLimit)).bindings.toList

      println("RESUTL" + results)

      if (!results.isEmpty) {
        val maxCount = results.head("count").value.toDouble
        for (result <- results if result.contains("t")) yield {
          (result("t").value, result("count").value.toDouble / maxCount)
        }
      } else {
        Traversable.empty
      }
    }
  }
}
