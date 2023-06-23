package org.silkframework.plugins.dataset.rdf.sparql


import org.silkframework.config.Prefixes
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlRestriction}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class SparqlRestrictionTest extends AnyFlatSpec with Matchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "freebase" -> "http://rdf.freebase.com/ns/"
  )

  "SparqlRestriction" should "convert prefixed URIS correctly" in {
    SparqlRestriction.fromSparql(SparqlEntitySchema.variable, "?a rdf:type freebase:musicartist").toSparql should
      equal("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/musicartist> .")

    SparqlRestriction.fromSparql(SparqlEntitySchema.variable, "?a rdf:type freebase:musicartist . ?a rdf:type freebase:musicartist2").toSparql should
      equal("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/musicartist> . ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/musicartist2> .")

    SparqlRestriction.fromSparql(SparqlEntitySchema.variable, "?a rdf:type freebase:music.artist").toSparql should
      equal("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/music.artist> .")
  }
}
