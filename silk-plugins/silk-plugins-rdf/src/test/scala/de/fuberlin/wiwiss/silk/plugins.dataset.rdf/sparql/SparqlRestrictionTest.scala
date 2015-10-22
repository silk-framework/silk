package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql

import de.fuberlin.wiwiss.silk.entity.rdf.SparqlRestriction
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.config.Prefixes

@RunWith(classOf[JUnitRunner])
class SparqlRestrictionTest extends FlatSpec with ShouldMatchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "freebase" -> "http://rdf.freebase.com/ns/"
  )

  "SparqlRestriction" should "convert prefixed URIS correctly" in {
    SparqlRestriction.fromSparql("a", "?a rdf:type freebase:musicartist").toSparql should
      equal("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/musicartist> .")

    SparqlRestriction.fromSparql("a", "?a rdf:type freebase:musicartist . ?a rdf:type freebase:musicartist2").toSparql should
      equal("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/musicartist> . ?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/musicartist2> .")

    SparqlRestriction.fromSparql("a", "?a rdf:type freebase:music.artist").toSparql should
      equal("?a <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://rdf.freebase.com/ns/music.artist> .")
  }
}
