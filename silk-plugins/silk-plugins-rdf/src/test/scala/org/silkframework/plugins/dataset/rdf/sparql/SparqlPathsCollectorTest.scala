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
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.silkframework.dataset.rdf.SparqlParams
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlRestriction}
import org.silkframework.entity._
import org.silkframework.entity.paths.{BackwardOperator, ForwardOperator, TypedPath}
import org.silkframework.plugins.dataset.rdf.datasets.SparqlDataset
import org.silkframework.plugins.dataset.rdf.endpoint.RemoteSparqlEndpoint
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Timer

class SparqlPathsCollectorTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  private val graphDBpedia = "http://example.org/dbpedia"
  private val graphSchemaOrg = "http://example.org/schemaOrg"

  implicit val userContext: UserContext = UserContext.Empty

  private lazy val endpoint = createEndpoint()

  private var fusekiServerInfo: Option[FusekiServerInfo] = None

  override def beforeAll(): Unit = {
    val dataset = DatasetFactory.createTxnMem()
    dataset.addNamedModel(graphDBpedia, loadData("test.nt"))
    dataset.addNamedModel(graphSchemaOrg, loadData("test2.nt"))
    fusekiServerInfo = Some(FusekiHelper.startFusekiServer(dataset, 3500))
  }

  override def afterAll(): Unit = {
    fusekiServerInfo foreach { info => info.server.stop() }
  }

  behavior of "SparqlPathsCollectorTest"

  it should "return all found paths from DBpedia excerpt sing aggregation collector" in {
    val paths = SparqlAggregatePathsCollector(endpoint, Some(graphDBpedia), SparqlRestriction.forType("http://dbpedia.org/ontology/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", UriValueType),
        forward("http://www.w3.org/2000/01/rdf-schema#label", StringValueType),
        backward("http://dbpedia.org/ontology/place", UriValueType)
      )
  }

  it should "return all found paths from DBpedia excerpt using sampling collector" in {
    val paths = SparqlSamplePathsCollector(endpoint, Some(graphDBpedia), SparqlRestriction.forType("http://dbpedia.org/ontology/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", StringValueType),
        forward("http://www.w3.org/2000/01/rdf-schema#label", StringValueType)
        // The sample path collector does not return backward paths: backward("http://dbpedia.org/ontology/place")
      )
  }

  it should "return all found paths from schema.org excerpt using aggregation collector" in {
    val paths = SparqlAggregatePathsCollector(endpoint, Some(graphSchemaOrg), SparqlRestriction.forType("http://schema.org/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", UriValueType),
        forward("http://www.w3.org/2000/01/rdf-schema#label", StringValueType)
      )
  }

  it should "return all found paths from schema.org excerpt using sampling collector" in {
    val paths = SparqlSamplePathsCollector(endpoint, Some(graphSchemaOrg), SparqlRestriction.forType("http://schema.org/City"), None)
    paths.toSet shouldBe
      Set(
        forward("http://www.w3.org/1999/02/22-rdf-syntax-ns#type", StringValueType),
        forward("http://www.w3.org/2000/01/rdf-schema#label", StringValueType)
      )
  }

  private def createEndpoint() = {
    val fusekiUrl = fusekiServerInfo.getOrElse(throw new RuntimeException("Did not start Fuseki server!")).url
    SparqlDataset(endpointURI = fusekiUrl).sparqlEndpoint
  }

  private def loadData(name: String): Model = {
    val stream = getClass.getClassLoader.getResourceAsStream("org/silkframework/plugins/dataset/rdf/" + name)
    val model = ModelFactory.createDefaultModel()
    model.read(stream, null, "TURTLE")
    model
  }

  private def forward(property: String, typ: ValueType) = TypedPath(ForwardOperator(property) :: Nil, typ, isAttribute = false)

  private def backward(property: String, typ: ValueType) = TypedPath(BackwardOperator(property) :: Nil, typ, isAttribute = false)

}

/**
 * Compares the performance of the different path collectors.
 */
object SparqlPathsCollectorBenchmark {
  implicit val logger: Logger = Logger.getLogger(getClass.getName)
  implicit val userContext: UserContext = UserContext.Empty

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

      val endpoint = RemoteSparqlEndpoint(SparqlParams(uri, retryCount = 100))
      val sparqlRestriction = SparqlRestriction.fromSparql(SparqlEntitySchema.variable, restriction)
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