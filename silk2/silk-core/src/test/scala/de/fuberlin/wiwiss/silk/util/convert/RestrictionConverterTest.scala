package de.fuberlin.wiwiss.silk.util.convert

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.instance.{Path, SparqlRestriction, Restriction}
import de.fuberlin.wiwiss.silk.instance.Restriction.{Or, Condition, And}

class RestrictionConverterTest extends FlatSpec with ShouldMatchers
{
  implicit val prefixes : Prefixes = Map(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "dbpedia" -> "http://dbpedia.org/ontology/",
    "lgdo" -> "http://linkedgeodata.org/ontology/"
  )

  "RestrictionConverter" should "convert simple patterns" in
  {
    val sparqlRestriction = SparqlRestriction.fromSparql("?a rdf:type dbpedia:Settlement")

    val restriction = Restriction(Some(Condition.resolve(Path.parse("?a/rdf:type"), Set("dbpedia:Settlement"))))

    RestrictionConverter("a", sparqlRestriction) should equal (restriction)
  }

  "RestrictionConverter" should "convert union patterns" in
  {
    val sparqlRestriction = SparqlRestriction.fromSparql("""
    {
       { ?b rdf:type lgdo:City }
       UNION
       { ?b rdf:type lgdo:Town }
       UNION
       { ?b rdf:type lgdo:Village }
    }
    """)

    val restriction = Restriction(Some(Or(
      Condition.resolve(Path.parse("?b/rdf:type"), Set("lgdo:City")) ::
      Condition.resolve(Path.parse("?b/rdf:type"), Set("lgdo:Town")) ::
      Condition.resolve(Path.parse("?b/rdf:type"), Set("lgdo:Village")) :: Nil
    )))

    RestrictionConverter("b", sparqlRestriction) should equal (restriction)
  }
}