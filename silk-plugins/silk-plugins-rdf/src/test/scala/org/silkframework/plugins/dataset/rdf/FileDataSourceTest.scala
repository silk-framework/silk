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

package org.silkframework.plugins.dataset.rdf

import java.io.File
import java.net.URLDecoder

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{EntitySchema, StringValueType, UriValueType, ValueType}
import org.silkframework.plugins.dataset.rdf.datasets.RdfFileDataset
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.FileResourceManager
import org.silkframework.util.Uri
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FileDataSourceTest extends AnyFlatSpec with Matchers {
  behavior of "File Data Source"

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "do" -> "http://dbpedia.org/ontology/"
  )

  implicit val userContext: UserContext = UserContext.Empty

  val fileName = "test.nt"

  val resourceLoader = FileResourceManager(new File(URLDecoder.decode(getClass.getClassLoader.getResource("org/silkframework/plugins/dataset/rdf").getFile, "UTF-8")))

  val dataset = RdfFileDataset(resourceLoader.get(fileName), "N-TRIPLE")

  val entityDescCity =
    EntitySchema(
      typeUri = Uri("http://dbpedia.org/ontology/City"),
      typedPaths = IndexedSeq(UntypedPath.parse("?a/rdfs:label").asStringTypedPath)
    )

  it should "return all cities" in {
    dataset.source.retrieve(entityDescCity).entities.size should equal (3)
  }

  it should "return entities by uri" in {
    dataset.source.retrieveByUri(entityDescCity, "http://dbpedia.org/resource/Berlin" :: Nil).entities.size should equal (1)
  }

  private val pathPlaces = UntypedPath.parse("?a/do:place/rdfs:label").asStringTypedPath

  private val pathPlacesCalledMunich = UntypedPath.parse("""?a/do:place[rdfs:label = "Munich"]/rdfs:label""").asStringTypedPath

  private val pathCities = UntypedPath.parse("""?a/do:place[rdf:type = do:City]/rdfs:label""").asStringTypedPath

  private val entityDescPerson =
    EntitySchema(
      typeUri = Uri("http://dbpedia.org/ontology/Person"),
      typedPaths = IndexedSeq(pathPlaces, pathPlacesCalledMunich, pathCities)
    )

  private val persons = dataset.source.retrieve(entityDescPerson).entities.toList

  it should "work with filters" in {
    persons.size should equal (1)
    persons.head.evaluate(pathPlaces).toSet should equal (Set("Berlin", "Munich", "Some Place"))
    persons.head.evaluate(pathPlacesCalledMunich).toSet should equal (Set("Munich"))
    persons.head.evaluate(pathCities).toSet should equal (Set("Berlin", "Munich"))
  }

  it should "return typed paths" in {
    dataset.source.retrievePaths("http://dbpedia.org/ontology/City").
        map(tp => tp.toUntypedPath.normalizedSerialization -> tp.valueType) shouldBe IndexedSeq(
          "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>" -> ValueType.URI,
          "<http://www.w3.org/2000/01/rdf-schema#label>" -> ValueType.STRING,
          "\\<http://dbpedia.org/ontology/place>" -> ValueType.URI
    )
  }
}