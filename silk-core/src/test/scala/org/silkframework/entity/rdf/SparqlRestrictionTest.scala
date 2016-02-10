package org.silkframework.entity.rdf

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes

class SparqlRestrictionTest extends FlatSpec with Matchers {

  implicit val prefixes = {
    Prefixes(Map(
      "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
      "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
      "owl" -> "http://www.w3.org/2002/07/owl#",
      "xsd" -> "http://www.w3.org/2001/XMLSchema#",
      "ex" -> "http://silkframework.org/example/"))
  }

  "SparqlRestriction" should "resolve prefixes" in {
    resolve("?a rdf:type ex:MyType .") should be ("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://silkframework.org/example/MyType> .")
  }

  it should "resolve type prefixes" in {
    resolve("?a ex:date \"2012-04-12T12:00:00Z\"^^xsd:dateTime .") should be ("?a <http://silkframework.org/example/date> \"2012-04-12T12:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .")
  }

  private def resolve(sparql: String) = SparqlRestriction.fromSparql("a", sparql).toSparql

}
