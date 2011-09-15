package de.fuberlin.wiwiss.silk.jena

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.instance.{Path, SparqlRestriction, InstanceSpecification}
import de.fuberlin.wiwiss.silk.config.Prefixes

class FileDataSourceTest extends FlatSpec with ShouldMatchers {

  implicit val prefixes: Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "dbpedia-owl" -> "http://dbpedia.org/ontology/"
  )

  val file = getClass.getClassLoader.getResource("de/fuberlin/wiwiss/silk/jena/test.nt").getFile

  val source = new FileDataSource(file, "N-TRIPLE")

  val instanceSpec =
    InstanceSpecification(
      variable = "a",
      restrictions = SparqlRestriction.fromSparql("?a rdf:type dbpedia-owl:City"),
      paths = IndexedSeq(Path.parse("?a/rdfs:label"))
    )

  "FileDataSource" should "return all cities" in {
    source.retrieve(instanceSpec).size should equal (3)
  }

  "FileDataSource" should "return entities by uri" in {
    source.retrieve(instanceSpec, "http://dbpedia.org/resource/Berlin" :: Nil).size should equal (1)
  }

  "FileDataSource" should "not return entities by uri which do not match the restriction" in {
    source.retrieve(instanceSpec, "http://dbpedia.org/resource/Berlin" :: "http://dbpedia.org/resource/Albert_Einstein" :: Nil).size should equal (1)
  }
}