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
class RestrictionConverterTest extends FlatSpec with ShouldMatchers {
  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "lgdo" -> "http://linkedgeodata.org/ontology/"
  )

  val restrictionConverter = new RestrictionConverter

  "RestrictionConverter" should "convert simple patterns" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("a", "?a rdf:type dbpedia:Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), Set("dbpedia:Settlement"))))

    restrictionConverter("a", sparqlRestriction) should equal(restriction)
  }

  "RestrictionConverter" should "convert simple patterns with any variable name" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("x", "?x rdf:type dbpedia:Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?x/rdf:type"), Set("dbpedia:Settlement"))))

    restrictionConverter("x", sparqlRestriction) should equal(restriction)
  }

  "RestrictionConverter" should "convert simple patterns with replacement" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("a", "?a a dbpedia:Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), Set("dbpedia:Settlement"))))

    restrictionConverter("a", sparqlRestriction) should equal(restriction)
  }

  "RestrictionConverter" should "convert simple patterns with special object" in {
    val sparqlRestriction = SparqlRestriction.fromSparql("a", "?a rdf:type ?Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), Set())))

    restrictionConverter("a", sparqlRestriction) should equal(restriction)
  }

  "RestrictionConverter" should "convert union patterns" in {
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
      Condition.resolve(Path.parse("?b/rdf:type"), Set("lgdo:City")) ::
      Condition.resolve(Path.parse("?b/rdf:type"), Set("lgdo:Town")) ::
      Condition.resolve(Path.parse("?b/rdf:type"), Set("lgdo:Village")) :: Nil
    )))

    restrictionConverter("b", sparqlRestriction) should equal(restriction)
  }
}