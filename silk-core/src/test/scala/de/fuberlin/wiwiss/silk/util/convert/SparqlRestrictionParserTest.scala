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

package de.fuberlin.wiwiss.silk.util.convert

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.entity.{Path, SparqlRestriction, Restriction}
import de.fuberlin.wiwiss.silk.entity.Restriction.{Or, Condition}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SparqlRestrictionParserTest extends FlatSpec with ShouldMatchers {
  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "lgdo" -> "http://linkedgeodata.org/ontology/"
  )

  val restrictionConverter = new SparqlRestrictionParser

  "SparqlRestrictionParser" should "parse an empty pattern" in {
    val sparqlRestriction1 = SparqlRestriction.fromSparql("a", "")
    val sparqlRestriction2 = SparqlRestriction.fromSparql("a", ". ")
    val restriction = Restriction(None)

    restrictionConverter(sparqlRestriction1) should equal(restriction)
    restrictionConverter(sparqlRestriction2) should equal(restriction)
  }

  "SparqlRestrictionParser" should "convert simple patterns" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("a", "?a rdf:type dbpedia:Settlement")
    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), "dbpedia:Settlement")))

    restrictionConverter(sparqlRestriction) should equal(restriction)
  }

  "SparqlRestrictionParser" should "convert simple patterns with full URIs" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("a", "?a rdf:type <http://unknown.org/Settlement>")
    val restriction = Restriction(Some(Condition(Path.parse("?a/rdf:type"), "http://unknown.org/Settlement")))

    restrictionConverter(sparqlRestriction) should equal(restriction)
  }

  "SparqlRestrictionParser" should "convert simple patterns with any variable name" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("x", "?x rdf:type dbpedia:Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?x/rdf:type"), "dbpedia:Settlement")))

    restrictionConverter(sparqlRestriction) should equal(restriction)
  }

  "SparqlRestrictionParser" should "convert simple patterns with type alias" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("a", "?a a dbpedia:Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), "dbpedia:Settlement")))

    restrictionConverter(sparqlRestriction) should equal(restriction)
  }

  "SparqlRestrictionParser" should "convert union patterns" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("b", """
       {{
        { ?b rdf:type lgdo:City }}
       UNION
       { ?b rdf:type lgdo:Town }
       UNION
       {  { ?b rdf:type lgdo:Village }
       } }
    """)

    val restriction = Restriction(Some(Or(
      Condition.resolve(Path.parse("?b/rdf:type"), "lgdo:City") ::
      Condition.resolve(Path.parse("?b/rdf:type"), "lgdo:Town") ::
      Condition.resolve(Path.parse("?b/rdf:type"), "lgdo:Village") :: Nil
    )))

    restrictionConverter(sparqlRestriction) should equal(restriction)
  }
}