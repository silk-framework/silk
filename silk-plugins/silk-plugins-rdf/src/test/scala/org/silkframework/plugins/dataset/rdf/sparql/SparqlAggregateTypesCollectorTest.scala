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

import org.apache.jena.query.DatasetFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, ShouldMatchers}
import org.silkframework.plugins.dataset.rdf.datasets.SparqlDataset
import org.silkframework.runtime.activity.UserContext

class SparqlAggregateTypesCollectorTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAll {

  private val graphDBpedia = "http://example.org/dbpedia"
  private val graphSchemaOrg = "http://example.org/schemaOrg"

  private lazy val endpoint = createEndpoint()

  implicit val userContext: UserContext = UserContext.Empty

  behavior of "SparqlAggregateTypesCollector"

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

  it should "return all found types from DBpedia excerpt" in {
    val typesWithFrequency = SparqlAggregateTypesCollector(endpoint, Some(graphDBpedia), None)
    val types = typesWithFrequency.map(_._1).toSet
    types shouldBe Set("http://dbpedia.org/ontology/City", "http://dbpedia.org/ontology/Person", "http://dbpedia.org/ontology/Place")
  }

  it should "return all found types from schema.org excerpt" in {
    val typesWithFrequency = SparqlAggregateTypesCollector(endpoint, Some(graphSchemaOrg), None)
    val types = typesWithFrequency.map(_._1).toSet
    types shouldBe Set("http://schema.org/City")
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

}