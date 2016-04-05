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

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.runtime.resource.FileResourceManager
import org.silkframework.util.Uri

class FileDataSourceTest extends FlatSpec with Matchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "do" -> "http://dbpedia.org/ontology/"
  )

  val fileName = "test.nt"

  val resourceLoader = new FileResourceManager(new File(URLDecoder.decode(getClass.getClassLoader.getResource("org/silkframework/plugins/dataset/rdf").getFile, "UTF-8")))

  val dataset = new FileDataset(resourceLoader.get(fileName), "N-TRIPLE")

  val entityDescCity =
    EntitySchema(
      typeUri = Uri("http://dbpedia.org/ontology/City"),
      paths = IndexedSeq(Path.parse("?a/rdfs:label"))
    )

  "FileDataSource" should "return all cities" in {
    dataset.source.retrieve(entityDescCity).size should equal (3)
  }

  "FileDataSource" should "return entities by uri" in {
    dataset.source.retrieveByUri(entityDescCity, "http://dbpedia.org/resource/Berlin" :: Nil).size should equal (1)
  }

  val pathPlaces = Path.parse("?a/do:place/rdfs:label")

  val pathPlacesCalledMunich = Path.parse("""?a/do:place[rdfs:label = "Munich"]/rdfs:label""")

  val pathCities = Path.parse("""?a/do:place[rdf:type = do:City]/rdfs:label""")

  val entityDescPerson =
    EntitySchema(
      typeUri = Uri("http://dbpedia.org/ontology/Person"),
      paths = IndexedSeq(pathPlaces, pathPlacesCalledMunich, pathCities)
    )

  val persons = dataset.source.retrieve(entityDescPerson).toList

  "FileDataSource" should "work with filters" in {
    persons.size should equal (1)
    persons.head.evaluate(pathPlaces).toSet should equal (Set("Berlin", "Munich", "Some Place"))
    persons.head.evaluate(pathPlacesCalledMunich).toSet should equal (Set("Munich"))
    persons.head.evaluate(pathCities).toSet should equal (Set("Berlin", "Munich"))
  }
}