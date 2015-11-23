package org.silkframework.util.convert

import org.silkframework.entity.rdf.SparqlRestrictionBuilder
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.silkframework.entity.{Path, Restriction}
import org.silkframework.entity.Restriction.Condition
import org.silkframework.config.Prefixes

@RunWith(classOf[JUnitRunner])
class SparqlRestrictionBuilderTest extends FlatSpec with ShouldMatchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "lgdo" -> "http://linkedgeodata.org/ontology/"
  )

  val builder = new SparqlRestrictionBuilder("a")

  "SparqlRestrictionBuilder" should "convert single conditions" in {
    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), "dbpedia:Settlement")))
    builder(restriction).toSparqlQualified should equal("{?a rdf:type dbpedia:Settlement} .")
  }
}
