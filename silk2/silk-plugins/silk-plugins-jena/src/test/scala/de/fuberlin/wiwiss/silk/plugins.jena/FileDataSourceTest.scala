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

package de.fuberlin.wiwiss.silk.plugins.jena

import org.scalatest.{Matchers, FlatSpec}
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, EntityDescription}
import de.fuberlin.wiwiss.silk.config.Prefixes
import java.io.File
import de.fuberlin.wiwiss.silk.runtime.resource.FileResourceManager

class FileDataSourceTest extends FlatSpec with Matchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "do" -> "http://dbpedia.org/ontology/"
  )

  val fileName = "test.nt"

  val resourceLoader = new FileResourceManager(new File(getClass.getClassLoader.getResource("de/fuberlin/wiwiss/silk/plugins/jena/datasource").getFile))

  val source = new FileDataSource(resourceLoader.get(fileName), "N-TRIPLE")

  val entityDescCity =
    EntityDescription(
      variable = "a",
      restrictions = SparqlRestriction.fromSparql("a", "?a rdf:type do:City"),
      paths = IndexedSeq(Path.parse("?a/rdfs:label"))
    )

  "FileDataSource" should "return all cities" in {
    source.retrieve(entityDescCity, Nil).size should equal (3)
  }

  "FileDataSource" should "return entities by uri" in {
    source.retrieve(entityDescCity, "http://dbpedia.org/resource/Berlin" :: Nil).size should equal (1)
  }

  val pathPlaces = Path.parse("?a/do:place/rdfs:label")

  val pathPlacesCalledMunich = Path.parse("""?a/do:place[rdfs:label = "Munich"]/rdfs:label""")

  val pathCities = Path.parse("""?a/do:place[rdf:type = do:City]/rdfs:label""")

  val entityDescPerson =
    EntityDescription(
      variable = "a",
      restrictions = SparqlRestriction.fromSparql("a", "?a rdf:type do:Person"),
      paths = IndexedSeq(pathPlaces, pathPlacesCalledMunich, pathCities)
    )

  val persons = source.retrieve(entityDescPerson, Nil).toList

  "FileDataSource" should "work with filters" in {
    persons.size should equal (1)
    persons.head.evaluate(pathPlaces) should equal (Set("Berlin", "Munich", "Some Place"))
    persons.head.evaluate(pathPlacesCalledMunich) should equal (Set("Munich"))
    persons.head.evaluate(pathCities) should equal (Set("Berlin", "Munich"))
  }
}