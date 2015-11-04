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

import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.SparqlParams
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import de.fuberlin.wiwiss.silk.util.Timer

/**
 * Compares the performance of the different path collectors.
 */
object SparqlTypesCollectorTest {
  implicit val logger = Logger.getLogger(getClass.getName)

  private val tests = {
    Test(
      name = "Sider",
      uri = "http://www4.wiwiss.fu-berlin.de/sider/sparql"
    ) :: Test(
      name = "DBpedia",
      uri = "http://dbpedia.org/sparql"
    ) :: Nil
  }

  def main(args: Array[String]) {
    for(test <- tests) test.execute()
  }

  private case class Test(name: String, uri: String) {

    def execute() {
      logger.info("Executing on " + uri + " test")

      val endpoint = new RemoteSparqlEndpoint(SparqlParams(uri= uri, retryCount = 100))

      val types = Timer("SparqlAggregateTypesCollector") {
        SparqlAggregateTypesCollector(endpoint, limit = None).toList
      }
      logger.info("Found " + types.size + " types: " + types.toString)
    }
  }
}