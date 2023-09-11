package org.silkframework.entity.rdf


import org.silkframework.config.Prefixes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SparqlRestrictionTest extends AnyFlatSpec with Matchers {

  implicit val prefixes: Prefixes = {
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

  it should "resolve prefixes with semicolons correctly" in {
    resolve("?a a ex:MyType; .") should be ("?a a <http://silkframework.org/example/MyType>; .")
  }

  it should "resolve type prefixes" in {
    resolve("?a ex:date \"2012-04-12T12:00:00Z\"^^xsd:dateTime .") should be ("?a <http://silkframework.org/example/date> \"2012-04-12T12:00:00Z\"^^<http://www.w3.org/2001/XMLSchema#dateTime> .")
  }

  it should "not complain about missing prefixes in literals" in {
    val restriction = "?a <http://www.example.com/someProperty> \"\"\"+fakePrefix:12* +prefix:23\"\"\" ."
    resolve(restriction) should be (restriction)
  }

  private def resolve(sparql: String) = SparqlRestriction.fromSparql("a", sparql).toSparql

}
