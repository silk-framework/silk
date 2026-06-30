package org.silkframework.plugins.dataset.rdf.tasks.templating

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class SparqlSelectVarExtractorTest extends AnyFlatSpec with Matchers {

  behavior of "SparqlSelectVarExtractor"

  it should "extract plain projected variables" in {
    extract("SELECT ?a ?b WHERE { ?a ?p ?b }") mustBe Seq("a", "b")
  }

  it should "strip DISTINCT" in {
    extract("SELECT DISTINCT ?x ?y WHERE { ?x ?p ?y }") mustBe Seq("x", "y")
  }

  it should "strip REDUCED" in {
    extract("SELECT REDUCED ?x ?y WHERE { ?x ?p ?y }") mustBe Seq("x", "y")
  }

  it should "return the alias of a single-expression projection" in {
    extract("SELECT (?x + 1 AS ?sum) WHERE { ?x ?p ?o }") mustBe Seq("sum")
  }

  it should "return only the outer AS alias for nested function calls" in {
    extract("SELECT (COUNT(?x) AS ?n) WHERE { ?x ?p ?o }") mustBe Seq("n")
  }

  it should "mix plain variables and AS aliases" in {
    extract("SELECT ?a (?x + 1 AS ?sum) ?b WHERE { ?a ?p ?b }") mustBe Seq("a", "sum", "b")
  }

  it should "fall back to all variables for SELECT *" in {
    extract("SELECT * WHERE { ?s ?p ?o }") mustBe Seq("s", "p", "o")
  }

  it should "fall back to all variables for SELECT * with a GRAPH clause" in {
    extract("SELECT * WHERE { GRAPH <urn:g> { ?s ?p ?o } }") mustBe Seq("s", "p", "o")
  }

  it should "be case-insensitive on SELECT / WHERE / DISTINCT / AS" in {
    extract("select distinct ?a (?x + 1 as ?sum) where { ?a ?p ?o }") mustBe Seq("a", "sum")
  }

  it should "tolerate a Jinja placeholder inside a string literal" in {
    extract("""SELECT ?s WHERE { ?s rdfs:label "{{ input.entity.name }}" }""") mustBe Seq("s")
  }

  it should "tolerate a Jinja placeholder in a numeric position" in {
    extract("SELECT ?s WHERE { ?s ?p ?o } LIMIT {{ input.config.max }}") mustBe Seq("s")
  }

  it should "tolerate a Jinja placeholder as a URI fragment" in {
    extract("SELECT ?s WHERE { ?s a <{{ input.config.type }}> }") mustBe Seq("s")
  }

  it should "accept projections terminated by a brace without a WHERE keyword" in {
    extract("SELECT ?a ?b { ?a ?p ?b }") mustBe Seq("a", "b")
  }

  it should "deduplicate while preserving first-appearance order" in {
    extract("SELECT ?a ?b ?a WHERE { ?a ?p ?b }") mustBe Seq("a", "b")
  }

  it should "return the outer projection, not inner sub-query variables" in {
    extract("SELECT ?a WHERE { SELECT ?x ?y WHERE { ?x ?p ?y } }") mustBe Seq("a")
  }

  it should "stop at FROM named graph clauses" in {
    extract("SELECT ?a ?b FROM <urn:g> WHERE { ?a ?p ?b }") mustBe Seq("a", "b")
  }

  it should "return an empty sequence for non-SELECT queries" in {
    extract("INSERT DATA { <urn:a> <urn:b> <urn:c> }") mustBe empty
  }

  it should "return an empty sequence for an ASK query" in {
    extract("ASK WHERE { ?s ?p ?o }") mustBe empty
  }

  private def extract(query: String): Seq[String] = SparqlSelectVarExtractor.extractSelectVars(query)
}
