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

import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.plugins.dataset.rdf.SparqlParams
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.util.Timer

/**
 * Compares the performance of the different path collectors.
 */
object SparqlPathsCollectorTest {
  implicit val logger = Logger.getLogger(getClass.getName)

  private val tests = {
    Test(
      name = "Sider",
      uri = "http://www4.wiwiss.fu-berlin.de/sider/sparql",
      restriction = "?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www4.wiwiss.fu-berlin.de/sider/resource/sider/drugs>"
    ) :: Test(
      name = "DBpedia-Drugs",
      uri = "http://dbpedia.org/sparql",
      restriction = "?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/ontology/Drug>"
    ) :: Nil
  }

  def main(args: Array[String]) {
    for(test <- tests) test.execute()
  }

  private case class Test(name: String, uri: String, restriction: String) {

    def execute() {
      logger.info("Executing " + name + " test")

      val endpoint = new RemoteSparqlEndpoint(SparqlParams(uri, retryCount = 100))
      val sparqlRestriction = SparqlRestriction.fromSparql("a", restriction)
      val limit = Some(50)

      Timer("SparqlAggregatePathsCollector") {
        SparqlAggregatePathsCollector(endpoint, sparqlRestriction, limit).toList
      }

      Timer("SparqlSamplePathsCollector") {
        SparqlSamplePathsCollector(endpoint, sparqlRestriction, limit).toList
      }
    }
  }
}