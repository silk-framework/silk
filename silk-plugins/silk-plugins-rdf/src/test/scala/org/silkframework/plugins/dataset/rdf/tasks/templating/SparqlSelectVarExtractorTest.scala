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

  it should "not require whitespace after a keyword" in {
    extract("SELECT DISTINCT* WHERE { ?s ?p ?o }") mustBe Seq("s", "p", "o")
    extract("SELECT ?a (COUNT(?x) AS?n) WHERE { ?a ?p ?x } GROUP BY ?a") mustBe Seq("a", "n")
  }

  it should "fall back to all variables for SELECT *" in {
    extract("SELECT * WHERE { ?s ?p ?o }") mustBe Seq("s", "p", "o")
  }

  it should "fall back to all variables for SELECT * with a GRAPH clause" in {
    extract("SELECT * WHERE { GRAPH <urn:g> { ?s ?p ?o } }") mustBe Seq("s", "p", "o")
  }

  it should "be case-insensitive on SELECT / WHERE / FROM / DISTINCT / AS" in {
    extract("select distinct ?a (?x + 1 as ?sum) from <urn:g> where { ?a ?p ?o }") mustBe Seq("a", "sum")
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
    extract("CONSTRUCT { ?s ?p ?o } WHERE { { SELECT ?s ?p ?o WHERE { ?s ?p ?o } } }") mustBe empty
  }

  it should "return an empty sequence for an ASK query" in {
    extract("ASK WHERE { ?s ?p ?o }") mustBe empty
  }

  it should "not mistake a variable named ?from for the FROM keyword" in {
    extract("SELECT ?to ?from WHERE { ?to ?p ?from }") mustBe Seq("to", "from")
  }

  it should "not mistake a variable named ?where for the WHERE keyword" in {
    extract("SELECT ?where ?x WHERE { ?where ?p ?x }") mustBe Seq("where", "x")
  }

  it should "not mistake a variable named ?from inside an expression for the FROM keyword" in {
    extract("SELECT ?to (COUNT(?from) AS ?n) WHERE { ?to ?p ?from }") mustBe Seq("to", "n")
  }

  it should "stop at a FROM clause that follows a variable named ?from" in {
    extract("SELECT ?from FROM <urn:g> WHERE { ?from ?p ?o }") mustBe Seq("from")
  }

  it should "extract variable names containing non-ASCII letters" in {
    extract("SELECT ?größe (MAX(?x) AS ?höhe) WHERE { ?größe ?p ?x }") mustBe Seq("größe", "höhe")
  }

  it should "support $-prefixed variables" in {
    extract("SELECT ?a $b (COUNT($c) AS $n) WHERE { ?a ?p $b }") mustBe Seq("a", "b", "n")
  }

  it should "ignore comments" in {
    extract("SELECT ?id\n  ?label # from rdfs:label, where available (see {docs})\n  ?type\nWHERE { ?id ?p ?label }") mustBe Seq("id", "label", "type")
    extract("# select all items from the graph\nSELECT ?a ?b WHERE { ?a ?p ?b }") mustBe Seq("a", "b")
    extract("# returns ?x\nSELECT * WHERE { ?s ?p ?o }") mustBe Seq("s", "p", "o")
    extract("SELECT ?a {# from the index #} ?b WHERE { ?a ?p ?b }") mustBe Seq("a", "b")
    extract("# queries {{ input.config.graph }}\nSELECT ?a ?b WHERE { ?a ?p ?b }") mustBe Seq("a", "b")
  }

  it should "not mistake a prefixed name for a keyword" in {
    extract("ASK { ?s ex:select ?o }") mustBe empty
    extract("PREFIX select: <urn:s#>\nSELECT * WHERE { ?s select:p ?o }") mustBe Seq("s", "o")
  }

  it should "not mistake IRI contents for comments or keywords" in {
    extract("SELECT * WHERE { ?s <http://example.org/ns#p> ?o }") mustBe Seq("s", "o")
    extract("PREFIX ex: <http://example.org/select/>\nPREFIX f: <http://example.org/from#>\nSELECT ?a WHERE { ?a ?p ?o }") mustBe Seq("a")
  }

  it should "skip escaped characters of prefixed names" in {
    extract("""SELECT * WHERE { dbr:Baldwin\'s_Peak ?p ?o }""") mustBe Seq("p", "o")
  }

  it should "ignore string literal contents" in {
    extract("""SELECT ?s (CONCAT("from ", ?x) AS ?y) WHERE { ?s ?p ?x }""") mustBe Seq("s", "y")
    extract("""SELECT ?a (GROUP_CONCAT(?x; separator=")") AS ?xs) WHERE { ?a ?p ?x } GROUP BY ?a""") mustBe Seq("a", "xs")
    extract("""SELECT * WHERE { ?s ?p "?x" . ?s <http://example.org/page?id=1> ?o }""") mustBe Seq("s", "p", "o")
  }

  it should "give up when a placeholder may change the result variables" in {
    extract("SELECT {{ input.config.vars }} WHERE { ?s ?p ?o }") mustBe empty
    extract("SELECT ?a {{ input.config.extraVars }} WHERE { ?a ?p ?o }") mustBe empty
    extract("SELECT * WHERE { ?s ?p ?o . {{ input.config.pattern }} }") mustBe empty
    // A LIMIT cannot bind variables, but the heuristic does not try to tell numeric positions apart
    extract("SELECT * WHERE { ?s ?p ?o } LIMIT {{ input.config.max }}") mustBe empty
    extract("{% if input.config.x %}\nSELECT ?a ?b WHERE { ?a ?p ?b }\n{% else %}\nSELECT ?a WHERE { ?a ?p ?o }\n{% endif %}") mustBe empty
  }

  it should "infer the schema when placeholders cannot change the result variables" in {
    extract("SELECT ?a FROM <{{ input.config.graph }}> WHERE { ?a ?p ?o {% if input.config.x %} FILTER(?o = 1) {% endif %} }") mustBe Seq("a")
    extract("""SELECT * WHERE { ?s a <{{ input.config.type }}> ; rdfs:label "{{ input.entity.name }}" }""") mustBe Seq("s")
  }

  it should "give up on SELECT * when a nested scope may bind variables that are not projected" in {
    extract("SELECT * WHERE { { SELECT ?a WHERE { ?a ?p ?hidden } } }") mustBe empty
    extract("SELECT * WHERE { ?s ?p ?o FILTER NOT EXISTS { ?s ?q ?hidden } }") mustBe empty
  }

  it should "give up on unbalanced parentheses in the projection" in {
    extract("SELECT ?a (COUNT(?x AS ?n WHERE { ?a ?p ?x }") mustBe empty
    extract("SELECT ?a ) ?b WHERE { ?a ?p ?b }") mustBe empty
  }

  it should "not fail on malformed input" in {
    extract("") mustBe empty
    extract("SELECT") mustBe empty
    noException must be thrownBy extract("SELECT ?a WHERE { ?a ?p \"unterminated }")
    noException must be thrownBy extract("SELECT ?a WHERE { ?a ?p {{ input.entity.name }")
  }

  private def extract(query: String): Seq[String] = SparqlSelectVarExtractor.extractSelectVars(query)
}
