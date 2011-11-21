/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.jena

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, EntityDescription}
import de.fuberlin.wiwiss.silk.config.Prefixes

class FileDataSourceTest extends FlatSpec with ShouldMatchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "dbpedia-owl" -> "http://dbpedia.org/ontology/"
  )

  val file = getClass.getClassLoader.getResource("de/fuberlin/wiwiss/silk/plugins/jena/datasource/test.nt").getFile

  val source = new FileDataSource(file, "N-TRIPLE")

  val entityDesc =
    EntityDescription(
      variable = "a",
      restrictions = SparqlRestriction.fromSparql("?a rdf:type dbpedia-owl:City"),
      paths = IndexedSeq(Path.parse("?a/rdfs:label"))
    )

  "FileDataSource" should "return all cities" in {
    source.retrieve(entityDesc).size should equal (3)
  }

  "FileDataSource" should "return entities by uri" in {
    source.retrieve(entityDesc, "http://dbpedia.org/resource/Berlin" :: Nil).size should equal (1)
  }

  "FileDataSource" should "not return entities by uri which do not match the restriction" in {
    source.retrieve(entityDesc, "http://dbpedia.org/resource/Berlin" :: "http://dbpedia.org/resource/Albert_Einstein" :: Nil).size should equal (1)
  }
}