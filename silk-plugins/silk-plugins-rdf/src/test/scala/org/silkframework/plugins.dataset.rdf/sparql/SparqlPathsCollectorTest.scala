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

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.dataset.rdf.SparqlParams
import org.silkframework.entity.{BackwardOperator, ForwardOperator, Path}
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.plugins.dataset.rdf.endpoint.{JenaDatasetEndpoint, RemoteSparqlEndpoint}
import org.silkframework.util.Timer

class SparqlPathsCollectorTest extends FlatSpec with ShouldMatchers {

  private val graphDBpedia = "http://example.org/dbpedia"
  private val graphSchemaOrg = "http://example.org/schemaOrg"

  private val endpoint = createEndpoint()

  behavior of "SparqlPathsCollectorTest"

  it should "return all found paths from DBpedia excerpt sing aggregation collector" in {
    val paths = SparqlAggregatePathsCollector(endpoint, Some(graphDBpedia), SparqlRestriction.forType("http://dbpedia.org/ontology/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
        forward("http://www.w3.org/2000/01/rdf-schema#label"),
        backward("http://dbpedia.org/ontology/place")
      )
  }

  it should "return all found paths from DBpedia excerpt using sampling collector" in {
    val paths = SparqlSamplePathsCollector(endpoint, Some(graphDBpedia), SparqlRestriction.forType("http://dbpedia.org/ontology/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
        forward("http://www.w3.org/2000/01/rdf-schema#label")
        // The sample path collector does not return backward paths: backward("http://dbpedia.org/ontology/place")
      )
  }

  it should "return all found paths from schema.org excerpt using aggregation collector" in {
    val paths = SparqlAggregatePathsCollector(endpoint, Some(graphSchemaOrg), SparqlRestriction.forType("http://schema.org/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
        forward("http://www.w3.org/2000/01/rdf-schema#label")
      )
  }

  it should "return all found paths from schema.org excerpt using sampling collector" in {
    val paths = SparqlSamplePathsCollector(endpoint, Some(graphSchemaOrg), SparqlRestriction.forType("http://schema.org/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
        forward("http://www.w3.org/2000/01/rdf-schema#label")
      )
  }

  private def createEndpoint() = {
    val dataset = DatasetFactory.createTxnMem()
    dataset.addNamedModel(graphDBpedia, loadData("test.nt"))
    dataset.addNamedModel(graphSchemaOrg, loadData("test2.nt"))
    new JenaDatasetEndpoint(dataset)
  }

  private def loadData(name: String): Model = {
    val stream = getClass.getClassLoader.getResourceAsStream("org/silkframework/plugins/dataset/rdf/" + name)
    val model = ModelFactory.createDefaultModel()
    model.read(stream, null, "TURTLE")
    model
  }

  private def forward(property: String) = Path(ForwardOperator(property) :: Nil)

  private def backward(property: String) = Path(BackwardOperator(property) :: Nil)

}

/**
 * Compares the performance of the different path collectors.
 */
object SparqlPathsCollectorBenchmark {
  implicit val logger: Logger = Logger.getLogger(getClass.getName)

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
        SparqlAggregatePathsCollector(endpoint, None, sparqlRestriction, limit).toList
      }

      Timer("SparqlSamplePathsCollector") {
        SparqlSamplePathsCollector(endpoint, None, sparqlRestriction, limit).toList
      }
    }
  }
}