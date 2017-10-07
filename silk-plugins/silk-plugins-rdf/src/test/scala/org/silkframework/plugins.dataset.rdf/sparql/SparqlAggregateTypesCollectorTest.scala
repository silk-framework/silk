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

import org.apache.jena.query.{Dataset, DatasetFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaDatasetEndpoint

class SparqlAggregateTypesCollectorTest extends FlatSpec with ShouldMatchers {

  private val graphDBpedia = "http://example.org/dbpedia"
  private val graphSchemaOrg = "http://example.org/schemaOrg"

  private val endpoint = createEndpoint()

  behavior of "SparqlAggregateTypesCollector"

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
    val dataset = DatasetFactory.createMem()
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

}