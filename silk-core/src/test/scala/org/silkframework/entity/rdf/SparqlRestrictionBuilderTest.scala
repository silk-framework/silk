package org.silkframework.entity.rdf


import org.silkframework.config.Prefixes
import org.silkframework.entity.Restriction.Condition
import org.silkframework.entity.Restriction
import org.silkframework.entity.paths.UntypedPath
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class SparqlRestrictionBuilderTest extends AnyFlatSpec with Matchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "lgdo" -> "http://linkedgeodata.org/ontology/"
  )

  val builder = new SparqlRestrictionBuilder(SparqlEntitySchema.variable)

  "SparqlRestrictionBuilder" should "convert single conditions" in {
    val restriction = Restriction(Some(Condition(UntypedPath.parse("?a/rdf:type"), prefixes.resolve("dbpedia:Settlement"))))
    builder(restriction).toSparqlQualified should equal("{?a rdf:type dbpedia:Settlement} .")
  }
}
